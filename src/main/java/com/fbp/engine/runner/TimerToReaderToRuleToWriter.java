package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import com.fbp.engine.protocol.ModbusTcpClient;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

// stage2. 5-1 시나리오 2
@Slf4j
public class TimerToReaderToRuleToWriter {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();
        simulator.setRegister(0, 350);
        log.info("시뮬레이터 구동 완료 (주소 0번 온도: 350 설정됨)");

        TimerNode timerNode = new TimerNode("timer", 2000);

        ModbusReaderNode readerNode = new ModbusReaderNode("modbus-in", Map.of(
                "host", "localhost"
                ,"port", 5020,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        ));

        TransformNode parseNode = new TransformNode("parse-data", msg -> {
            int[] regs = (int[]) msg.getPayload().get("registers");
            Map<String, Object> newPayload = new HashMap<>();
            newPayload.put("temperature", regs != null && regs.length > 0 ? regs[0] / 10.0 : 0.0);
            return new Message(newPayload);
        });

        RuleNode ruleNode = new RuleNode("rule-temp", msg -> {
            Double temp = (Double) msg.getPayload().get("temperature");
            return temp != null && temp > 30.0;
        });

        ModbusWriterNode writerNode = new ModbusWriterNode("modbus-out", Map.of(
                "host", "localhost",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "alertStatus",
                "scale", 1.0
        ));

        TransformNode makeControlMsgNode = new TransformNode("make-control", msg -> {
            Map<String, Object> newPayload = new HashMap<>(msg.getPayload());
            newPayload.put("alertStatus", 1);
            return new Message(newPayload);
        });

        LogNode logNode = new LogNode("log-normal");

        Connection timerToReader = new Connection();
        Connection readerToParse = new Connection();
        Connection parseToRule = new Connection();
        Connection ruleToControl = new Connection();
        Connection controlToWriter = new Connection();
        Connection ruleToLog = new Connection();

        timerNode.getOutputPort("out").connect(timerToReader);
        readerNode.getOutputPort("out").connect(readerToParse);
        parseNode.getOutputPort("out").connect(parseToRule);
        ruleNode.getOutputPort("match").connect(ruleToControl);
        makeControlMsgNode.getOutputPort("out").connect(controlToWriter);
        ruleNode.getOutputPort("mismatch").connect(ruleToLog);

        timerNode.initialize();
        readerNode.initialize();
        parseNode.initialize();
        ruleNode.initialize();
        makeControlMsgNode.initialize();
        writerNode.initialize();
        logNode.initialize();

        new Thread(() -> {
            try {
                while(true) {
                    if (timerToReader.getBufferSize() > 0) {
                        readerNode.process(timerToReader.poll());
                    }
                    if (readerToParse.getBufferSize() > 0) {
                        parseNode.process(readerToParse.poll());
                    }
                    if (parseToRule.getBufferSize() > 0) {
                        ruleNode.process(parseToRule.poll());
                    }
                    if (ruleToControl.getBufferSize() > 0) {
                        makeControlMsgNode.process(ruleToControl.poll());
                    }
                    if (controlToWriter.getBufferSize() > 0) {
                        writerNode.process(controlToWriter.poll());
                        log.info("시뮬레이터 2번 주소 값 변경 됨: {}", simulator.getRegister(2));

                        simulator.setRegister(0, 250);
                        log.info("온도를 25.0도로 변경");
                    }
                    if (ruleToLog.getBufferSize() > 0) {
                        logNode.process(ruleToLog.poll());
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                log.warn("Exception in TimerToReaderToRuleToWriter", e);
            }
        }).start();

    }
}
