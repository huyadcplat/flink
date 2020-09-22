package org.apache.flink.metrics.huya;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.Metric;
import org.apache.flink.metrics.MetricConfig;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.metrics.reporter.AbstractReporter;
import org.apache.flink.metrics.reporter.Scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.HOST;
import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.JOBID;
import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.NAME;
import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.NODE;
import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.PORT;
import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.URI;

/**
 * report flink metric to huya monitor system.
 */
public class HuyaMonitorReportor extends AbstractReporter implements Scheduled {

	private static final Logger LOG = LoggerFactory.getLogger(HuyaMonitorReportor.class);
	private static final String HOST_VARIABLE = "<host>";

	private String name;
	private String jobId;
	private String node;
	private HttpClient client;
	private Map<String, String> configTags = new HashMap<>();

	protected final Map<Gauge<?>, DGauge> gauges = new ConcurrentHashMap<>();
	protected final Map<Counter, DCounter> counters = new ConcurrentHashMap<>();
	protected final Map<Histogram, String> histograms = new ConcurrentHashMap<>();
	protected final Map<Meter, DMeter> meters = new ConcurrentHashMap<>();

	@Override
	public void notifyOfAddedMetric(Metric metric, String metricName, MetricGroup group) {
		final String name = group.getMetricIdentifier(metricName, this);

		Map<String, String> tags = new HashMap<>(configTags);
		tags.putAll(getTagsFromMetricGroup(group));
		String host = getHostFromMetricGroup(group);

		synchronized (this) {
			if (metric instanceof Counter) {
				Counter c = (Counter) metric;
				counters.put(c, new DCounter(c, name, host, tags));
			} else if (metric instanceof Gauge) {
				Gauge g = (Gauge) metric;

				gauges.put(g, new DGauge(g, name, host, tags));
			} else if (metric instanceof Meter) {
				Meter m = (Meter) metric;
				// Only consider rate
				meters.put(m, new DMeter(m, name, host, tags));
			} else {
				log.warn("Cannot add unknown metric type {}. This indicates that the reporter " +
					"does not support this metric type.", metric.getClass().getName());
			}
		}
	}

	@Override
	public void notifyOfRemovedMetric(Metric metric, String metricName, MetricGroup group) {
		synchronized (this) {
			if (metric instanceof Counter) {
				counters.remove(metric);
			} else if (metric instanceof Gauge) {
				gauges.remove(metric);
			} else if (metric instanceof Histogram) {
				histograms.remove(metric);
			} else if (metric instanceof Meter) {
				meters.remove(metric);
			} else {
				log.warn("Cannot remove unknown metric type {}. This indicates that the reporter " +
					"does not support this metric type.", metric.getClass().getName());
			}
		}
	}

	@Override
	public String filterCharacters(String str) {
		char[] chars = null;
		final int strLen = str.length();
		int pos = 0;
		for (int i = 0; i < strLen; i++) {
			final char c = str.charAt(i);
			switch (c) {
				case '>':
				case '<':
				case '"':
					// remove character by not moving cursor
					if (chars == null) {
						chars = str.toCharArray();
					}
					break;
				case ' ':
					if (chars == null) {
						chars = str.toCharArray();
					}
					chars[pos++] = '_';
					break;
				case ',':
				case '=':
				case ';':
				case ':':
				case '?':
				case '\'':
				case '*':
					if (chars == null) {
						chars = str.toCharArray();
					}
					chars[pos++] = '-';
					break;
				default:
					if (chars != null) {
						chars[pos] = c;
					}
					pos++;
			}
		}
		return chars == null ? str : new String(chars, 0, pos);
	}

	@Override
	public void open(MetricConfig config) {
		name = config.getString(NAME.key(), NAME.defaultValue());
		if (name == null) {
			throw new IllegalArgumentException("Invalid name configuration. name: " + name);
		}

		jobId = config.getString(JOBID.key(), JOBID.defaultValue());
		if (jobId == null) {
			throw new IllegalArgumentException("Invalid jobId configuration. jobId: " + jobId);
		}
		node = config.getString(NODE.key(), NODE.defaultValue());
		if (node == null) {
			throw new IllegalArgumentException("Invalid node configuration. node: " + node);
		}
		String host = config.getString(HOST.key(), HOST.defaultValue());
		if (host == null) {
			throw new IllegalArgumentException("Invalid host configuration. host: " + host);
		}
		int port = config.getInteger(PORT.key(), PORT.defaultValue());
		String address = String.format("http://%s:%d", host, port);
		String uri = config.getString(URI.key(), URI.defaultValue());
		client = new HttpClient(address, uri);
		log.info("Configured HuyaMonitorReportor with {address:{}, uri path:{}}", address, uri);
	}

	@Override
	public void close() {
		client.close();
	}

	@Override
	public void report() {
		HttpRequestEntity entity = new HttpRequestEntity(name, jobId, node);
		for (DMeter dMeter : meters.values()) {
			entity.addMetric(dMeter);
		}
		for (DGauge dGauge : gauges.values()) {

			entity.addMetric(dGauge);
		}
		for (DCounter dCounter : counters.values()) {
			entity.addMetric(dCounter);
		}
		try {
			client.send(entity.getReportList());
		} catch (SocketTimeoutException e) {
			LOG.warn("Failed reporting metrics  because of socket timeout.", e.getMessage());
		} catch (Exception e) {
			LOG.warn("Failed reporting metrics.", e);
		}
	}

	private String getHostFromMetricGroup(MetricGroup metricGroup) {
		return metricGroup.getAllVariables().get(HOST_VARIABLE);
	}

	/**
	 * Get tags from MetricGroup#getAllVariables(), excluding 'host'.
	 */
	private Map<String, String> getTagsFromMetricGroup(MetricGroup metricGroup) {
		Map<String, String> tags = new HashMap<>();

		for (Map.Entry<String, String> entry : metricGroup.getAllVariables().entrySet()) {
			if (!entry.getKey().equals(HOST_VARIABLE)) {
				tags.put(entry.getKey(), entry.getValue());
			}
		}

		return tags;
	}

	/**
	 * Removes leading and trailing angle brackets.
	 */
	private String getVariableName(String str) {
		return str.substring(1, str.length() - 1);
	}

}
