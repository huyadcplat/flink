package org.apache.flink.metrics.huya;

import org.apache.flink.metrics.*;
import org.apache.flink.metrics.reporter.AbstractReporter;
import org.apache.flink.metrics.reporter.Scheduled;
import org.apache.flink.runtime.metrics.groups.AbstractMetricGroup;
import org.apache.flink.runtime.metrics.groups.FrontMetricGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.apache.flink.metrics.huya.HuyaMonitorReportorOptions.*;

/** report flink metric to huya monitor system. */
public class HuyaMonitorReportor extends AbstractReporter implements Scheduled {

    private static final Logger LOG = LoggerFactory.getLogger(HuyaMonitorReportor.class);

    private String namespace;
    private String jobId;
    private int step = 60;
    private HttpClient client;
    private static final Pattern UNALLOWED_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9:_]");
    private static final String DELIMITE = "_";
    private String metricUrl;
    private Set<String> metricsSet = new HashSet<>();
    private AtomicLong time = new AtomicLong(0);

    protected final Map<Gauge<?>, DGauge> gauges = new ConcurrentHashMap<>();
    protected final Map<Counter, DCounter> counters = new ConcurrentHashMap<>();
    protected final Map<Histogram, String> histograms = new ConcurrentHashMap<>();
    protected final Map<Meter, DMeter> meters = new ConcurrentHashMap<>();

    @Override
    public void notifyOfAddedMetric(Metric metric, String metricName, MetricGroup group) {
        synchronized (this) {
            final String name =
                    filter(
                            ((FrontMetricGroup<AbstractMetricGroup<?>>) group).getLogicalScope(this)
                                    + DELIMITE
                                    + metricName);
            Map<String, String> tags = getTags(group);
            if (metric instanceof Counter) {
                Counter c = (Counter) metric;
                counters.put(c, new DCounter(c, name, tags));
            } else if (metric instanceof Gauge) {
                Gauge g = (Gauge) metric;
                gauges.put(g, new DGauge(g, name, tags));
            } else if (metric instanceof Meter) {
                Meter m = (Meter) metric;
                // Only consider rate
                meters.put(m, new DMeter(m, name, tags));
            } else {
                log.warn(
                        "Cannot add unknown metric type {}. This indicates that the reporter "
                                + "does not support this metric type.",
                        metric.getClass().getName());
            }
        }
    }

    private static Map<String, String> getTags(MetricGroup group) {
        // Keys are surrounded by brackets: remove them, transforming "<name>" to "name".
        Map<String, String> tags = new HashMap<>();
        for (Map.Entry<String, String> variable : group.getAllVariables().entrySet()) {
            String name = variable.getKey();
            String value = variable.getValue();
            if (value.length() > 60) {
                value = value.substring(0, 50) + "-" + Md5Utils.crypt(value);
            }
            tags.put(name.substring(1, name.length() - 1), value);
        }
        return tags;
    }

    @Override
    public String filterCharacters(String str) {
        // Only [a-zA-Z0-9:_.] are valid in metric names, any other characters should be sanitized
        // to an underscore.
        return filter(str);
    }

    public static String filter(String str) {
        return UNALLOWED_CHAR_PATTERN.matcher(str).replaceAll(DELIMITE);
    }

    @Override
    public void open(MetricConfig config) {
        namespace = config.getString(NAMESPACE.key(), NAMESPACE.defaultValue());
        if (NAMESPACE == null) {
            throw new IllegalArgumentException(
                    "Invalid name configuration. NAMESPACE: " + namespace);
        }

        jobId = config.getString(JOB_ID.key(), JOB_ID.defaultValue());
        if (jobId == null) {
            throw new IllegalArgumentException("Invalid jobId configuration. jobId: " + jobId);
        }
        step = config.getInteger(STEP.key(), STEP.defaultValue());
        metricUrl = config.getString(METRIC_URL.key(), METRIC_URL.defaultValue());
        log.info("Configured HuyaMonitorReportor with metricUrl:{}", metricUrl);
        String address = config.getString(URL.key(), URL.defaultValue());
        if (address == null) {
            throw new IllegalArgumentException("Invalid host configuration. url: " + address);
        }

        client = new HttpClient(address);
        log.info("Configured HuyaMonitorReportor with address:{}", address);

        metricsSet = client.getRequiredMetrics(metricUrl);
        log.info("get metrics size={}, detail={}", metricsSet.size(), metricsSet.toArray());
    }

    @Override
    public void close() {
        client.close();
    }

    private boolean filterMetric(DMetric metric) {
        return metricsSet.contains(metric.getMetric());
    }

    @Override
    public void report() {
        HttpRequestEntity entity = new HttpRequestEntity(namespace, jobId, step);
        List<Gauge> gaugesToRemove = new ArrayList<>();
        for (Map.Entry<Gauge<?>, DGauge> entry : gauges.entrySet()) {
            DGauge g = entry.getValue();
            if (g.getMetricValue() == null) {
                gaugesToRemove.add(entry.getKey());
            }
        }
        gaugesToRemove.forEach(gauges::remove);
        for (DMeter dMeter : meters.values()) {
            if (filterMetric(dMeter)) {
                entity.addMetric(dMeter);
            }
        }
        for (DGauge dGauge : gauges.values()) {
            if (filterMetric(dGauge)) {
                entity.addMetric(dGauge);
            }
        }
        for (DCounter dCounter : counters.values()) {
            if (filterMetric(dCounter)) {
                entity.addMetric(dCounter);
            }
        }
        try {
            client.send(entity.getReportList());
        } catch (SocketTimeoutException e) {
            LOG.warn("Failed reporting metrics  because of socket timeout.", e.getMessage());
        } catch (Exception e) {
            LOG.warn("Failed reporting metrics.", e);
        }

        long interval = time.addAndGet(1);
        if (interval % 5 == 0) {
            Set<String> set = client.getRequiredMetrics(metricUrl);
            if (set.size() > 0) {
                synchronized (metricsSet) {
                    metricsSet = set;
                }
            }
            if (interval % 20 == 0) {
                log.info("get metrics size={}, detail={}", metricsSet.size(), metricsSet.toArray());
            }
        }
    }
}
