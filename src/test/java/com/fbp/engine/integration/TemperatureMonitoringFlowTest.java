package com.fbp.engine.integration;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.TemperatureSensorNode;
import com.fbp.engine.node.ThresholdFilterNode;
import com.fbp.engine.node.TimerNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TemperatureMonitoringFlowTest {
    static class CollectorNode extends AbstractNode {

        private final List<Message> messages = new CopyOnWriteArrayList<>();
        public CollectorNode(String id) {
            super(id);
            addInputPort("in");
        }

        @Override
        protected void onProcess(Message message) {
            messages.add(message);
        }

        public List<Message> getMessages() {
            return messages;
        }
    }

    private List<Message> alerts;
    private List<Message> normals;

    @BeforeEach
    void setUpFlowAndRun() throws InterruptedException {
        FlowEngine engine = new FlowEngine();

        CollectorNode alertCollector = new CollectorNode("alert-collector");
        CollectorNode normalCollector = new CollectorNode("normal-collector");

        Flow flow = new Flow("integrated-test")
                .addNode(new TimerNode("timer", 10))
                .addNode(new TemperatureSensorNode("sensor", 15.0, 45.0))
                .addNode(new ThresholdFilterNode("filter", "temperature", 30.0))
                .addNode(alertCollector)
                .addNode(normalCollector)
                .connect("timer", "out", "sensor", "trigger")
                .connect("sensor", "out", "filter", "in")
                .connect("filter", "alert", "alert-collector", "in")
                .connect("filter", "normal", "normal-collector", "in");
        engine.register(flow);

        List<Thread> workers = new ArrayList<>();
        for (Flow.FlowConnection fc : flow.getConnections()) {
            AbstractNode targetNode = flow.getNodes().get(fc.getTargetNodeId());
            Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Message msg = fc.getConnection().poll();
                        if (msg != null && flow.getState() == Flow.FlowState.RUNNING) {
                            targetNode.process(msg);
                        } else {
                            Thread.sleep(2);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            workers.add(thread);
            thread.start();
        }
        engine.startFlow("integrated-test");
        Thread.sleep(500);
        engine.shutdown();
        workers.forEach(Thread::interrupt);

        this.alerts = alertCollector.getMessages();
        this.normals = normalCollector.getMessages();

    }

    @Test
    @DisplayName("alert 경로 검증")
    void test1() {
        Assertions.assertFalse(alerts.isEmpty());

        for (Message msg : alerts) {
            double temp = (double) msg.get("temperature");
            Assertions.assertTrue(temp > 30.0);
        }
    }

    @Test
    @DisplayName("normal 경로 검증")
    void test2() {
        Assertions.assertFalse(normals.isEmpty());

        for (Message msg : normals) {
            double temp = (double) msg.get("temperature");
            Assertions.assertTrue(temp <= 30.0);
        }
    }

    @Test
    @DisplayName("전체 메시지 수")
    void test3() {
        int totalProcessed = alerts.size() + normals.size();

        Assertions.assertTrue(totalProcessed > 0);
    }
}
