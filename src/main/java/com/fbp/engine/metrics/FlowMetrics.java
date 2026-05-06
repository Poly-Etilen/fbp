package com.fbp.engine.metrics;

import java.util.List;

public record FlowMetrics(
        String flowId,
        List<NodeMetricsSnapshot> nodes,
        long totalProcessed,
        long totalErrors
) {
    public record NodeMetricsSnapshot(
            String id,
            long processed,
            long errors,
            double avgTimeMs
    ) {}
}
