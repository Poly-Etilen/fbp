package com.fbp.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {
    private MetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new MetricsCollector();
    }

    @Test
    @DisplayName("처리 건수 기록") //
    void testRecordProcessingSuccess() {
        collector.recordProcessing("node1", System.nanoTime(), true);
        NodeMetrics metrics = collector.getMetrics("node1");
        
        assertNotNull(metrics);
        assertEquals(1, metrics.getProcessedCount().get());
        assertEquals(0, metrics.getErrorCount().get());
    }

    @Test
    @DisplayName("에러 건수 기록")
    void testRecordProcessingFailure() {
        collector.recordProcessing("node1", System.nanoTime(), false);
        NodeMetrics metrics = collector.getMetrics("node1");
        
        assertEquals(0, metrics.getProcessedCount().get());
        assertEquals(1, metrics.getErrorCount().get());
    }

    @Test
    @DisplayName("평균 처리 시간")
    void testAverageProcessingTime() {
        long startTime = System.nanoTime();
        collector.recordProcessing("node1", startTime - 10_000_000L, true);
        collector.recordProcessing("node1", startTime - 20_000_000L, true);

        NodeMetrics metrics = collector.getMetrics("node1");
        assertEquals(15.0, metrics.getAverageProcessingTimeMs(), 1.0); //
    }

    @Test
    @DisplayName("멀티스레드 안전성")
    void testMultiThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    collector.recordProcessing("multi-node", System.nanoTime(), true);
                }
                latch.countDown();
            });
        }

        latch.await();
        service.shutdown();

        NodeMetrics metrics = collector.getMetrics("multi-node");
        assertEquals(threadCount * iterationsPerThread, metrics.getProcessedCount().get()); //
    }

    @Test
    @DisplayName("노드별 분리")
    void testNodeSeparation() {
        collector.recordProcessing("node1", System.nanoTime(), true);
        collector.recordProcessing("node2", System.nanoTime(), true);
        collector.recordProcessing("node2", System.nanoTime(), false);

        assertEquals(1, collector.getMetrics("node1").getProcessedCount().get());
        assertEquals(1, collector.getMetrics("node2").getProcessedCount().get());
        assertEquals(1, collector.getMetrics("node2").getErrorCount().get());
    }

    @Test
    @DisplayName("리셋")
    void testReset() {
        collector.recordProcessing("node1", System.nanoTime(), true);
        collector.reset();

        assertNull(collector.getMetrics("node1"));
    }

    @Test
    @DisplayName("존재하지 않는 노드") //
    void testNonExistentNode() {
        assertNull(collector.getMetrics("unknown-node"));
    }

}