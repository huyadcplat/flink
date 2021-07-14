package org.apache.flink.metrics.huya;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.metrics.HistogramStatistics;

import java.util.Map;

public class DHistogram extends DMetric {
    private final Histogram histogram;


    public DHistogram(Histogram histogram, String metric, Map<String, String> tags) {
        super(MetricType.histogram, metric, tags);
        this.histogram = histogram;
    }

    @Override
    public Double getMetricValue() {
        return null;
    }

    public HistogramStatistics getStatistics() {
        return histogram.getStatistics();
    }

}
