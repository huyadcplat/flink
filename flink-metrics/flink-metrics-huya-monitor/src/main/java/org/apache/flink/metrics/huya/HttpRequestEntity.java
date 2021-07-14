package org.apache.flink.metrics.huya;

import org.apache.flink.metrics.HistogramStatistics;
import org.apache.flink.runtime.metrics.dump.MetricDump;

import scala.collection.mutable.History;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HttpRequestEntity {
    private String namespace;
    private String jobId;
    private int step;

    public HttpRequestEntity(String namespace, String jobId, int step) {
        this.namespace = namespace;
        this.jobId = jobId;
        this.step = step;
    }

    private List<DMetric> metrics = new ArrayList<>();

    public List<DMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DMetric> metrics) {
        this.metrics = metrics;
    }

    public void addMetric(DMetric metric) {
        metrics.add(metric);
    }


    public List<ReportRequest> getReportList() {
        List<ReportRequest> reportObjs = new ArrayList<>();
        for (DMetric dMetric : metrics) {
            ReportRequest reportObj;
            if (dMetric instanceof DHistogram) {
                DHistogram dHistogram = (DHistogram)dMetric;
                HistogramStatistics histogramStatistics = dHistogram.getStatistics();
                Map<String, Double> target = new HashMap<>();
                target.put("_min", Double.valueOf(histogramStatistics.getMin()));
                target.put("_max", Double.valueOf(histogramStatistics.getMax()));
                target.put("_mean", Double.valueOf(histogramStatistics.getMean()));
                target.put("_stddev", Double.valueOf(histogramStatistics.getStdDev()));
                target.put("_p75", Double.valueOf(histogramStatistics.getQuantile(0.75)));
                target.put("_p95", Double.valueOf(histogramStatistics.getQuantile(0.95)));
                target.put("_p98", Double.valueOf(histogramStatistics.getQuantile(0.98)));
                target.put("_p99", Double.valueOf(histogramStatistics.getQuantile(0.99)));
                target.put("_p999", Double.valueOf(histogramStatistics.getQuantile(0.999)));
                for (Map.Entry<String, Double> entry: target.entrySet()) {
                    reportObj = new ReportRequest()
                            .setNamespace(namespace)
                            .setMetric(dMetric.getMetric() + entry.getKey())
                            .setEndpoint(jobId)
                            .setStep(step)
                            .setTimestamp(DMetric.getUnixEpochTimestamp())
                            .setTags(dMetric.getStringTags())
                            .setValue(entry.getValue());
                    reportObjs.add(reportObj);
                }
                continue;
            } else {
                reportObj = new ReportRequest()
                        .setNamespace(namespace)
                        .setMetric(dMetric.getMetric())
                        .setEndpoint(jobId)
                        .setStep(step)
                        .setTimestamp(DMetric.getUnixEpochTimestamp())
                        .setTags(dMetric.getStringTags())
                        .setValue(dMetric.getMetricValue());
                reportObjs.add(reportObj);
            }
        }
        return reportObjs;
    }
}
