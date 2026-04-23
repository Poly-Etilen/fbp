package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.node.TransformNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

// stage2. 5-1 시나리오 3
@Slf4j
public class MqttToRuleToModbus {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();

        MqttSubscriberNode subNode = new MqttSubscriberNode("mqtt-sub", Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "sub-sensor-cross",
                "topic", "sensor/temp"
        ));

        RuleNode ruleNode = new RuleNode("rule-temp", msg -> {
            Number temp = (Number) msg.getPayload().get("temperature");
            return temp != null && temp.doubleValue() > 30.0;
        });

        TransformNode transformNode = new TransformNode("transform-to-modbus", msg -> {
            Map<String, Object> newPayload = new HashMap<>(msg.getPayload());
            newPayload.put("fanSpeed", 100);
            return new Message(newPayload);
        });

        ModbusWriterNode writerNode = new ModbusWriterNode("modbus-writer", Map.of(
                "host", "localhost",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 5,
                "valueField", "fanSpeed",
                "scale", 1.0
        ));

        Connection subToRule = new Connection();
        Connection ruleToTransform = new Connection();
        Connection transformToWriter = new Connection();

        subNode.getOutputPort("out").connect(subToRule);
        ruleNode.getOutputPort("match").connect(ruleToTransform);
        transformNode.getOutputPort("out").connect(transformToWriter);

        subNode.initialize();
        ruleNode.initialize();
        transformNode.initialize();
        writerNode.initialize();

        new Thread(() -> {
            try {
                while (true) {
                    if (subToRule.getBufferSize() > 0) {
                        ruleNode.process(subToRule.poll());
                    }
                    if (ruleToTransform.getBufferSize() > 0) {
                        transformNode.process(ruleToTransform.poll());
                    }
                    if (transformToWriter.getBufferSize() > 0) {
                        writerNode.process(transformToWriter.poll());
                    }
                }
            } catch (Exception e) {
                log.warn("Exception in MqttToRuleToModbus", e);
            }
        }).start();
    }
}
