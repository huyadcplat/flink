package org.apache.flink.metrics.huya;

import org.apache.flink.metrics.Meter;

import java.util.Map;

/**
 * Mapping of meter between Flink and Datadog.
 *
 * <p>Only consider rate of the meter, due to Datadog HTTP API's limited support of meter
 */
public class DMeter extends DMetric {
    private final Meter meter;

    public DMeter(Meter m, String metricName, Map<String, String> tags) {
        super(MetricType.gauge, metricName, tags);
        meter = m;
    }

    @Override
    public Double getMetricValue() {
        return meter.getRate();
    }
}
