package com.fbp.engine.practice;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.ThresholdFilterNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// stage2, 2-7
@Slf4j
public class MqttBidirectionalApp {
    public static void main(String[] args) throws InterruptedException{
        FlowEngine engine = new FlowEngine();

        Map<String, Object> subConfig = new HashMap<>();
        subConfig.put("brokerUrl", "tcp://localhost:1883");
        subConfig.put("clientId", "test2-sub-client");
        subConfig.put("topic", "sensor/temp");

        Map<String, Object> pubConfig = new HashMap<>();
        pubConfig.put("brokerUrl", "tcp://localhost:1883");
        pubConfig.put("clientId", "test2-pub-client");
        pubConfig.put("topic", "alert/temp");

        Flow flow = new Flow("mqtt-bidirectional-flow", "mqtt 양방향 플로우")
                .addNode(new MqttSubscriberNode("sub", subConfig))
                .addNode(new ThresholdFilterNode("filter", "temperature", 30.0))
                .addNode(new MqttPublisherNode("pub", pubConfig))
                .connect("sub", "out", "filter", "in")
                .connect("filter", "alert", "pub", "in");

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
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "worker-" + fc.getId());
            workers.add(thread);
            thread.start();
        }

        log.info("양방향 MQTT 플로우 가동 시작");
        engine.startFlow("mqtt-bidirectional-flow");

        Thread.sleep(30000);
        log.info("종료 요청");
        engine.shutdown();
        workers.forEach(Thread::interrupt);
    }
}
