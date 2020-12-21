package org.apache.flink.metrics.huya;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author
 */
@Data
public abstract class DMetric {
	private static final long MILLIS_TO_SEC = 1000L;

	private final String metric;
	private final MetricType type;
	private final Map<String, String> tags;

	public DMetric(MetricType metricType, String metric, Map<String, String> tags) {
		this.type = metricType;
		this.metric = metric;
		this.tags = tags;
	}

	public String getMetric() {
		return metric;
	}

	public String getStringTags() {
		List<String> tagsList = new ArrayList<>();
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			tagsList.add(entry.getKey() + "=" + entry.getValue());
		}
		return StringUtils.join(tagsList, ",");
	}

	@JsonIgnore
	public abstract Double getMetricValue();

	public static long getUnixEpochTimestamp() {
		return (System.currentTimeMillis() / MILLIS_TO_SEC);
	}
}
