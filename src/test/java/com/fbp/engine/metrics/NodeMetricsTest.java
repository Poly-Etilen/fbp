package com.fbp.engine.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeMetricsTest {

    @Test
    @DisplayName("초기값")
    void testInitialValues() {
        NodeMetrics metrics = new NodeMetrics("test-node");

        Assertions.assertEquals("test-node", metrics.getNodeId());
        Assertions.assertEquals(0, metrics.getProcessedCount().get());
        Assertions.assertEquals(0, metrics.getErrorCount().get());
        Assertions.assertEquals(0.0, metrics.getAverageProcessingTimeMs());
    }

    @Test
    @DisplayName("increment")
    void testIncrement() {
        NodeMetrics metrics = new NodeMetrics("test-node");

        metrics.recordProcessed(100_000_000L);
        Assertions.assertEquals(1, metrics.getProcessedCount().get());

        metrics.recordFailure();
        Assertions.assertEquals(1, metrics.getErrorCount().get());
    }

    @Test
    @DisplayName("평균 계산")
    void testAverageCalculation() {
        NodeMetrics metrics = new NodeMetrics("test-node");

        metrics.recordProcessed(10_000_000L);
        metrics.recordProcessed(20_000_000L);

        double expectedAvg = 15.0;
        Assertions.assertEquals(expectedAvg, metrics.getAverageProcessingTimeMs(), 0.001);
    }

    @Test
    @DisplayName("평균 계산")
    void testAverageWithZeroCount() {
        NodeMetrics metrics = new NodeMetrics("test-node");
        Assertions.assertEquals(0.0, metrics.getAverageProcessingTimeMs());
    }
}