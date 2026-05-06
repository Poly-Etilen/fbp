package com.fbp.engine.metrics;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
public class NodeMetrics {
    private final String nodeId;
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    public NodeMetrics(String nodeId) {
        this.nodeId = nodeId;
    }

    public void recordProcessed(long durationNs) {
        processedCount.incrementAndGet();
        totalProcessingTime.addAndGet(durationNs);
    }

    public void recordFailure() {
        errorCount.incrementAndGet();
    }

    public double getAverageProcessingTimeMs() {
        long count = processedCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (totalProcessingTime.get() / (double) count) / 1_000_000.0;
    }
}
