package org.apache.flink.streaming.runtime.connector.metrics;

import java.util.Map;
import org.apache.flink.metrics.Gauge;



public class ConsumeDelayGauge implements Gauge<Long> {

    private volatile long defaultConsumeDelay = -1;
    private final Map<String, Long> produceTimestampMap;

    private volatile long value = Long.MIN_VALUE;
    private final String key;

    public ConsumeDelayGauge(Map<String, Long> produceTimestampMap, String key) {
        this.produceTimestampMap = produceTimestampMap;
        this.key = key;
    }

    public void setValue(long value) {
        this.value = (value > 0 ? value : defaultConsumeDelay);
    }

    @Override
    public Long getValue() {
        return value;
    }
}
