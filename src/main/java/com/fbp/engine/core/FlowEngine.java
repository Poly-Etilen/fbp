package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.text.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class FlowEngine {
    public enum State {
        INITIALIZED,
        RUNNING,
        STOPPED
    }
    @Getter
    private final Map<String, Flow> flows;
    @Getter
    private State state;

    private final ExecutorService executor;

    public FlowEngine() {
        this.flows = new HashMap<>();
        this.state = State.INITIALIZED;
        this.executor = Executors.newFixedThreadPool(20);
    }

    public void register(Flow flow) {
        flows.put(flow.getId(), flow);
        log.info("[Engine] Flow {}를 등록됨", flow.getId());
    }

    public void listFlows() {
        log.info("[Engine] 현재 등록된 플로우 목록");
        if (flows.isEmpty()) {
            log.info(" - 등록된 플로우가 없음");
        } else {
            for (Flow flow : flows.values()) {
                log.info(" - ID: [{}] | Status: [{}]", flow.getId(), flow.getState());
            }
        }
    }

    public void startFlow(String flowId) {
        Flow flow = flows.get(flowId);

        if (flow == null) {
            throw new IllegalArgumentException("Flow with id " + flowId + " not found");
        }

        if (flow.getState() == Flow.FlowState.RUNNING) {
            log.warn("[Engine] 플로우 '{}'는 이미 실행 중", flowId);
            return;
        }
        List<String> errors = flow.validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Flow with id " + flowId + " validation errors: " + errors);
        }

        flow.initialize();
        for (Flow.FlowConnection flowConnection : flow.getConnections()) {
            Connection conn = flowConnection.getConnection();

            executor.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Message msg = conn.poll();
                        if (conn.getTarget() != null) {
                            conn.getTarget().receive(msg);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        flow.setState(Flow.FlowState.RUNNING);
        this.state = State.RUNNING;
        log.info("Engine] 플로우 '{}' 시작됨", flowId);
    }

    public void stopFlow(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow != null) {
            flow.shutdown();
            flow.setState(Flow.FlowState.STOPPED);
            log.info("[Engine] 플로우 '{}' 정지됨", flowId);
        }
    }

    public void shutdown() {
        for (Flow flow : flows.values()) {
            if (flow.getState() == Flow.FlowState.RUNNING) {
                flow.shutdown();
                flow.setState(Flow.FlowState.STOPPED);
            }
        }
        this.state = State.STOPPED;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

}
