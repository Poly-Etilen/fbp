package com.fbp.engine.practice;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;


// stage2, 2-5
@Slf4j
public class MqttSubscriberApp {
    public static void main(String[] args) {
        Map<String, Object> mqttConfig = Map.of(
                "breakerUrl", "tcp://localhost:1883",
                "clientId", "app-sub-" + UUID.randomUUID().toString(),
                "topic", "sensor/test",
                "qos", 1
        );

        MqttSubscriberNode mqttNode = new MqttSubscriberNode("MQTT-IN", mqttConfig);
        PrintNode printer = new PrintNode("PRINT-OUT");

        Flow flow = new Flow("mqtt-test-flow");
        flow.addNode(mqttNode)
                .addNode(printer)
                .connect("MQTT-IN", "out", "PRINT-OUT", "in");

        FlowEngine engine = new FlowEngine();
        engine.register(flow);

        log.info("starting");
        engine.startFlow(flow.getId());
    }
}
