package org.apache.flink.streaming.runtime.connector.metrics;

import org.apache.flink.metrics.Gauge;

import java.util.Map;

public class RecordTimestampGauge implements Gauge<Long> {

    private volatile long defaultConsumeTimestamp = -1;
    private final Map<String, Long> produceTimestampMap;

    private final String key;


    public RecordTimestampGauge(Map<String, Long> produceTimestampMap, String key) {
        this.produceTimestampMap = produceTimestampMap;
        this.key = key;
    }

    @Override
    public Long getValue() {
        return produceTimestampMap.getOrDefault(key, defaultConsumeTimestamp);
    }
}
