package org.apache.flink.streaming.runtime.connector.metrics;

public class ProduceTimeGauge {

    private final ConsumeDelayGauge consumeDelayGauge;
    private final RecordTimestampGauge recordTimestampGauge;

    public ProduceTimeGauge(
            ConsumeDelayGauge consumeDelayGauge,
            RecordTimestampGauge recordTimestampGauge) {
        this.consumeDelayGauge = consumeDelayGauge;
        this.recordTimestampGauge = recordTimestampGauge;
    }

    public void gauge(long timestamp) {
        consumeDelayGauge.setValue(System.currentTimeMillis() - timestamp);
        recordTimestampGauge.setValue(timestamp);
    }

    @Override
    public String toString() {
        return "ProduceTimeGauge{" +
                "consumeDelay=" + consumeDelayGauge.getValue() +
                ", recordTimestamp=" + recordTimestampGauge.getValue() +
                '}';
    }
}
