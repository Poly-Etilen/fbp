package com.fbp.engine.practice;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.MqttPublisherNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;


// stage2, 2-6
@Slf4j
public class MqttPublisherApp {
    public static void main(String[] args) {
        Map<String, Object> config = Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "app-pub-" + UUID.randomUUID().toString(),
                "topic", "sensor/out",
                "qos", 1
        );

        GeneratorNode generatorNode = new GeneratorNode("GEN-IN");
        MqttPublisherNode publisherNode = new MqttPublisherNode("MQTT-OUT", config);

        Flow flow = new Flow("mqtt-pub-flow");

        flow.addNode(generatorNode)
                .addNode(publisherNode)
                .connect("GEN-IN", "out", "MQTT-OUT", "in");

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        log.info("Engin Start");
        engine.startFlow(flow.getId());

        int idCounter = 1;
        while (true) {
            try {
                generatorNode.generate("temperature", 25.0 + (idCounter % 5));
                log.info("데이터 생성됨 (ID: {})", idCounter);

                idCounter++;
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.info("generate 중지");
                break;
            }
        }
    }
}
