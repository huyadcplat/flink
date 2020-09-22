/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.metrics.huya;

import org.apache.flink.metrics.Gauge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapping of gauge between Flink and Datadog.
 */
public class DGauge extends DMetric {

	private static final Logger LOG = LoggerFactory.getLogger(DGauge.class);

	private final Gauge<?> gauge;

	public DGauge(Gauge g, String metricName, String host, Map<String, String> tags) {
		super(MetricType.gauge, metricName, host, tags);
		gauge = g;
	}

	/**
	 * Visibility of this method must not be changed
	 * since we deliberately not map it to json object in a Datadog-defined format.
	 */
	@Override
	public Number getMetricValue() {
		final Object value = gauge.getValue();
		if (value == null) {
			LOG.warn("Gauge {} is null-valued, defaulting to 0.", gauge);
			return 0;
		}
		if (value instanceof Number) {
			return (Number) value;
		}
		if (value instanceof Boolean) {
			return ((Boolean) value) ? 1 : 0;
		}
		LOG.warn("Invalid type for Gauge {}: {}, only number types and booleans are supported by this reporter.",
			gauge, value.getClass().getName());
		return 0;
	}
}
