package org.apache.flink.metrics.huya;

import java.util.ArrayList;
import java.util.List;


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
			ReportRequest reportObj = new ReportRequest()
				.setNamespace(namespace)
				.setMetric(dMetric.getMetric())
				.setEndpoint(jobId)
				.setStep(step)
				.setTimestamp(DMetric.getUnixEpochTimestamp())
				.setTags(dMetric.getStringTags())
				.setValue(dMetric.getMetricValue());
			reportObjs.add(reportObj);
		}
		return reportObjs;
	}
}
