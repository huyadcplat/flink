package org.apache.flink.metrics.huya;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;


/**
 * Config options for the {@link HuyaMonitorReportor}.
 */
public class HuyaMonitorReportorOptions {

	public static final ConfigOption<String> NAME = ConfigOptions
		.key("name")
		.noDefaultValue()
		.withDescription("The HuyaMonitor system metadata name");

	public static final ConfigOption<String> HOST = ConfigOptions
		.key("host")
		.noDefaultValue()
		.withDescription("The HuyaMonitoir report server host.");

	public static final ConfigOption<Integer> PORT = ConfigOptions
		.key("port")
		.defaultValue(80)
		.withDescription("The HuyaMonitoir report server port.");

	public static final ConfigOption<String> URI = ConfigOptions
		.key("uri")
		.defaultValue("/")
		.withDescription("The HuyaMonitoir report server uri.");

	public static final ConfigOption<String> JOBID = ConfigOptions
		.key("job-id")
		.noDefaultValue()
		.withDescription("The HuyaMonitoir report job-id.");

	public static final ConfigOption<String> NODE = ConfigOptions
		.key("node")
		.noDefaultValue()
		.withDescription("The HuyaMonitoir report node type: (taskExecutor/jobMaster)");

}
