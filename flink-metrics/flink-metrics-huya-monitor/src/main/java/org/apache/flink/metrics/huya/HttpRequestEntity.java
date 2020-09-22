package org.apache.flink.metrics.huya;

import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class HttpRequestEntity {
	private String name;
	private String jobId;
	private String node;

	public HttpRequestEntity(String name, String jobId, String node) {
		this.name = name;
		this.jobId = jobId;
		this.node = node;
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

	public List<ReportObj> getReportList() {
		List<ReportObj> reportObjs = new ArrayList<>();
		for (DMetric dMetric : metrics) {
			ReportObj reportObj = new ReportObj();
			reportObj.setMetricName(name);
			reportObj.setHost(dMetric.getHost());
			reportObj.setJobId(jobId);
			reportObj.setNode(node);
			reportObj.setName(dMetric.getMetric());
			reportObj.setValue(dMetric.getMetricValue());
			reportObj.setIts(dMetric.getUnixEpochTimestamp());
			reportObjs.add(reportObj);
		}
		return reportObjs;
	}
}
