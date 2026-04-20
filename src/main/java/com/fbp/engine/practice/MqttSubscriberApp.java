package com.fbp.engine.practice;

import com.fbp.engine.core.Connection;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.PrintNode;

import java.util.Map;

public class MqttSubscriberApp {
    public static void main(String[] args) {
        Map<String, Object> mqttConfig = Map.of(
                "breakerUrl", "tcp://localhost:1883",
                "clientId", "app-sub-1",
                "topic", "sensor/test",
                "qos", 1
        );

        MqttSubscriberNode mqttNode = new MqttSubscriberNode("MQTT-IN", mqttConfig);
        PrintNode printer = new PrintNode("PRINT-OUT");

        Connection conn = new Connection();

    }
}
