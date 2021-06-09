package org.apache.flink.streaming.runtime.connector.metrics;

import org.apache.flink.metrics.Gauge;

import java.util.Map;

public class ConsumeDelayGauge implements Gauge<Long> {

    private volatile long defaultConsumeDelay = -1;
    private final Map<String, Long> produceTimestampMap;

    private final String key;

    public ConsumeDelayGauge(Map<String, Long> produceTimestampMap, String key) {
        this.produceTimestampMap = produceTimestampMap;
        this.key = key;
    }

    @Override
    public Long getValue() {
        long current = System.currentTimeMillis();
        Long produceTime = produceTimestampMap.get(key);
        return produceTime == null? null: current-produceTime;
    }
}
