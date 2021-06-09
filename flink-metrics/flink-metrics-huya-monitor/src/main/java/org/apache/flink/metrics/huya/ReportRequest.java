package org.apache.flink.metrics.huya;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** */
@Data
@Accessors(chain = true)
public class ReportRequest implements Serializable {
    private String namespace;
    /** metric为jobId. */
    private String metric;
    /** endpoint为指标名. */
    private String endpoint;

    private int step;
    private long timestamp;
    private Double value;
    private String tags;
}
