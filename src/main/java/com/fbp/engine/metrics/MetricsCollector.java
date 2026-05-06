package com.fbp.engine.metrics;

import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsCollector {
    private final Map<String, NodeMetrics> nodeMetricsMap = new ConcurrentHashMap<>();

    public void recordProcessing(String nodeId, long startTimeNs, boolean success) {
        NodeMetrics metrics = nodeMetricsMap.computeIfAbsent(nodeId, NodeMetrics::new);
        if (success) {
            metrics.recordProcessed(System.nanoTime() - startTimeNs);
        } else {
            metrics.recordFailure();
        }
    }

    public NodeMetrics getMetrics(String nodeId) {
        return nodeMetricsMap.get(nodeId);
    }

    public Map<String, NodeMetrics> getAllNodeMetrics() {
        return nodeMetricsMap;
    }

    public void reset() {
        nodeMetricsMap.clear();
    }

    public FlowMetrics createFlowMetrics(FlowDefinition definition) {
        List<FlowMetrics.NodeMetricsSnapshot> snapshots = new ArrayList<>();
        long totalProcessed = 0;
        long totalErrors = 0;

        for (NodeDefinition nodeDef : definition.getNodes()) {
            String nodeId = nodeDef.getId();
            NodeMetrics metrics = nodeMetricsMap.getOrDefault(nodeId, new NodeMetrics(nodeId));

            long processed = metrics.getProcessedCount().get();
            long errors = metrics.getErrorCount().get();

            snapshots.add(new FlowMetrics.NodeMetricsSnapshot(nodeId, processed, errors, metrics.getAverageProcessingTimeMs()));
            totalProcessed += processed;
            totalErrors += errors;
        }
        return new FlowMetrics(definition.getId(), snapshots, totalProcessed, totalErrors);
    }
}
