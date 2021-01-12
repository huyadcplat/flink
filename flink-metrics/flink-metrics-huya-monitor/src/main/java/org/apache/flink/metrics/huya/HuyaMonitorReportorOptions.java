package org.apache.flink.metrics.huya;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;


/**
 * Config options for the {@link HuyaMonitorReportor}.
 */
public class HuyaMonitorReportorOptions {

	public static final ConfigOption<String> NAMESPACE = ConfigOptions
		.key("namespace")
		.defaultValue("realtime.platform")
		.withDescription("The HuyaMonitor system metadata namespace");

	/**
	 * 上报url: 默认配置为  http://neo-transfer.huya.info/api/v1/push
	 */
	public static final ConfigOption<String> URL = ConfigOptions
		.key("url")
		.defaultValue("http://neo-transfer.huya.info/api/v1/push")
		.withDescription("The HuyaMonitoir report server url.");

	public static final ConfigOption<String> METRIC_URL = ConfigOptions
		.key("metric-url")
		.defaultValue("https://stream-dc.huya.com/svr/metrics/getAllMetrics.do")
		.withDescription("The HuyaMonitoir report server url.");

	public static final ConfigOption<String> JOB_ID = ConfigOptions
		.key("job-id")
		.noDefaultValue()
		.withDescription("The HuyaMonitoir report job-id.");

	public static final ConfigOption<Integer> STEP = ConfigOptions
		.key("step")
		.defaultValue(60)
		.withDescription("The HuyaMonitoir report step.");
}
