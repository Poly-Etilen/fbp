package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.*;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

// stage2. 과제 4-4
@Slf4j
public class MqttModbusIntegrationRunner {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();
        log.debug("MODBUS 시뮬레이터 시작, 초기 레지스터[2] 값: {}", simulator.getRegister(2));

        try {
            MqttSubscriberNode mqttSub = new MqttSubscriberNode("mqtt-sub", Map.of(
                    "brokerUrl", "tcp://localhost:1883",
                    "clientId", "rule-engine-sub",
                    "topic", "sensor/temp"
            ));

            RuleNode ruleNode = new RuleNode("rule-node", "temperature > 30.0");
            LogNode logNode = new LogNode("log-node");
            MqttPublisherNode mqttPub = new MqttPublisherNode("mqtt-pub", Map.of(
                    "brokerUrl", "tcp://localhost:1883",
                    "clientId", "rule-engine-pub",
                    "topic", "alert/temp"
            ));

            TransformNode transformNode = new TransformNode("transform-node", msg -> msg.withEntry("fanCommand", 1));
            TransformNode removeTopicNode = new TransformNode("remove-topic-node", msg -> msg.withoutKey("topic"));
            ModbusWriterNode modbusWriter = new ModbusWriterNode("modbus-writer", Map.of(
                    "host", "127.0.0.1",
                    "port", 5020,
                    "slaveId", 1,
                    "registerAddress", 2,
                    "valueField", "fanCommand"
            ));

            Flow flow = new Flow("mqtt-modbus-flow", "");
            flow.addNode(mqttSub)
                    .addNode(ruleNode)
                    .addNode(logNode)
                    .addNode(mqttPub)
                    .addNode(transformNode)
                    .addNode(removeTopicNode)
                    .addNode(modbusWriter);
            flow.connect("mqtt-sub", "out", "rule-node", "in");
            flow.connect("rule-node", "mismatch", "log-node", "in");
            flow.connect("rule-node", "match", "remove-topic-node", "in");
            flow.connect("remove-topic-node", "out", "mqtt-pub", "in");
            flow.connect("rule-node", "match", "transform-node", "in");
            flow.connect("transform-node", "out", "modbus-writer", "in");

            FlowEngine engine = new FlowEngine();
            engine.register(flow);
            engine.startFlow("mqtt-modbus-flow");

            log.info("통합 엔진 구동 완료");
            Thread.sleep(30000);

            log.info("최종 MODBUS 레지스터[2] 값: {}", simulator.getRegister(2));
            engine.shutdown();
        } catch (Exception e) {
            log.error("실행 중 오류 발생", e);
        } finally {
            simulator.stop();
        }
    }
}
