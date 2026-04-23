package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.node.TransformNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// stage2. 5-3
@Slf4j
public class PerformanceTestRunner {

    public static void main(String[] args) throws Exception {
        // 테스트할 메시지 건수 (100건, 500건, 1000건 + 10000건 추가)
        int[] testCases = {100, 500, 1000, 10000};

        for (int targetCount : testCases) {
            runPerformanceTest(targetCount);
            Thread.sleep(1000);
        }

        log.info("=== 성능 측정 테스트 종료 ===");
    }

    private static void runPerformanceTest(int targetCount) {
        log.info("--- [성능 측정 시작] 목표 처리량: {}건 ---", targetCount);

        TransformNode parseNode = new TransformNode("parse-node", msg -> {
            Map<String, Object> payload = new HashMap<>(msg.getPayload());
            payload.put("temperature", Math.random() * 50.0);
            return new Message(payload);
        });

        RuleNode ruleNode = new RuleNode("rule-node", msg -> {
            Double temp = (Double) msg.getPayload().get("temperature");
            return temp != null && temp > 30.0;
        });

        Connection parseToRule = new Connection();
        Connection ruleToMatch = new Connection();
        Connection ruleToMismatch = new Connection();

        parseNode.getOutputPort("out").connect(parseToRule);
        ruleNode.getOutputPort("match").connect(ruleToMatch);
        ruleNode.getOutputPort("mismatch").connect(ruleToMismatch);

        parseNode.initialize();
        ruleNode.initialize();

        List<Long> latencies = new ArrayList<>(targetCount);
        int processedCount = 0;
        long totalStartTime = System.nanoTime();

        try {
            for (int i = 0; i < targetCount; i++) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("creationTimeNanos", System.nanoTime());
                Message inputMsg = new Message(payload);
                parseNode.process(inputMsg);

                if (parseToRule.getBufferSize() > 0) {
                    ruleNode.process(parseToRule.poll());
                }

                Message resultMsg = null;
                if (ruleToMatch.getBufferSize() > 0) {
                    resultMsg = ruleToMatch.poll();
                } else if (ruleToMismatch.getBufferSize() > 0) {
                    resultMsg = ruleToMismatch.poll();
                }

                if (resultMsg != null) {
                    long creationTimeNanos = (long) resultMsg.getPayload().get("creationTimeNanos");
                    long latencyNanos = System.nanoTime() - creationTimeNanos;
                    latencies.add(latencyNanos);
                    processedCount++;
                }
            }
        } catch (InterruptedException e) {
            log.warn("에러 발생: ",e);
        }

        long totalElapsedNanos = System.nanoTime() - totalStartTime;
        double totalElapsedSeconds = totalElapsedNanos / 1_000_000_000.0;
        double throughput = targetCount / totalElapsedSeconds;

        double avgLatencyMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double maxLatencyMs = latencies.stream().mapToLong(Long::longValue).max().orElse(0L) / 1_000_000.0;
        double minLatencyMs = latencies.stream().mapToLong(Long::longValue).min().orElse(0L) / 1_000_000.0;

        log.info(">>> [결과] 총 처리 시간: {}초", String.format("%.4f", totalElapsedSeconds));
        log.info(">>> [결과] Throughput (처리량): {} msg/s", String.format("%,.2f", throughput));
        log.info(">>> [결과] Latency (지연시간): 평균 {}ms (최소 {}ms / 최대 {}ms)\n",
                String.format("%.4f", avgLatencyMs),
                String.format("%.4f", minLatencyMs),
                String.format("%.4f", maxLatencyMs));
    }
}