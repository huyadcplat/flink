package org.apache.flink.metrics.huya;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class ReportObj {

	@JsonProperty("metric_name")
	private String metricName;
	private long its;
	@JsonProperty("job_id")
	private String jobId;
	private String node;
	@JsonProperty("tm_id")
	private String tmId;
	private String host;
	private String name;
	private Number value;

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public long getIts() {
		return its;
	}

	public void setIts(long its) {
		this.its = its;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getTmId() {
		return tmId;
	}

	public void setTmId(String tmId) {
		this.tmId = tmId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Number getValue() {
		return value;
	}

	public void setValue(Number value) {
		this.value = value;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}
}
