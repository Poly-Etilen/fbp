package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

// stage2. 5-1 시나리오 1
@Slf4j
public class MqttToRuleToMqttRunner {
    public static void main(String[] args) {
        MqttSubscriberNode subNode = new MqttSubscriberNode("mqtt-sub", Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "sub-sensor",
                "topic", "sensor/temp"
        ));

        RuleNode ruleNode = new RuleNode("rule-temp", msg -> {
            Number temp = (Number) msg.getPayload().get("value");
            return temp != null && temp.doubleValue() > 30.0;
        });

        TransformNode transformNode = new TransformNode("transform-topic", msg -> {
            Map<String, Object> newPayload = new HashMap<>(msg.getPayload());
            newPayload.remove("topic");
            newPayload.put("alertMessage", "High temperature 감지");
            return new Message(newPayload);
        });

        MqttPublisherNode pubNode = new MqttPublisherNode("mqtt-pub", Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "pub-alert",
                "topic", "alert/temp"
        ));

        LogNode logNode = new LogNode("log-normal");

        Connection subToRule = new LocalConnection();
        Connection ruleToTransform = new LocalConnection();
        Connection transformToPub = new LocalConnection();
        Connection ruleToLog = new LocalConnection();

        subNode.getOutputPort("out").connect(subToRule);
        ruleNode.getOutputPort("match").connect(ruleToTransform);
        transformNode.getOutputPort("out").connect(transformToPub);
        ruleNode.getOutputPort("mismatch").connect(ruleToLog);

        subNode.initialize();
        ruleNode.initialize();
        transformNode.initialize();
        pubNode.initialize();
        logNode.initialize();

        new Thread(() -> {
            try {
                while(true) {
                    if (subToRule.getBufferSize() > 0) {
                        ruleNode.process(subToRule.poll());
                    }
                    if (ruleToTransform.getBufferSize() > 0) {
                        transformNode.process(ruleToTransform.poll());
                    }
                    if (transformToPub.getBufferSize() > 0) {
                        pubNode.process(transformToPub.poll());
                    }
                    if (ruleToLog.getBufferSize() > 0) {
                        logNode.process(ruleToLog.poll());
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                log.warn("Exception in MqttToRuleToMqttRunner", e);
            }
        }).start();

        log.info("MqttToRuleToMqttRunner 대기 중...");
        log.info("에: mosquitto_pub -t \"sensor/temp\" -m '{\"value\": 35.0}'");
    }
}
