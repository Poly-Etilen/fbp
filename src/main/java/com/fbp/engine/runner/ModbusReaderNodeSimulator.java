package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ModbusReaderNodeSimulator {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.setRegister(0, 255);
        simulator.setRegister(1, 600);
        simulator.setRegister(2, 1);
        simulator.start();

        try {
            Thread.sleep(500);
            TimerNode timerNode = new TimerNode("timer", 2000L);

            Map<String, Object> readerConfig = new HashMap<>();
            readerConfig.put("host", "localhost");
            readerConfig.put("port", 5020);
            readerConfig.put("slaveId", 1);
            readerConfig.put("startAddress", 0);
            readerConfig.put("count", 3);
            ModbusReaderNode readerNode = new ModbusReaderNode("reader", readerConfig);

            PrintNode printNode = new PrintNode("printer");

            Flow flow = new Flow("flow", "플로우");
            flow.addNode(timerNode);
            flow.addNode(readerNode);
            flow.addNode(printNode);

            flow.connect("timer", "out", "reader", "trigger");
            flow.connect("reader", "out", "printer", "in");
            flow.connect("reader", "error", "printer", "in");

            FlowEngine engine = new FlowEngine();
            engine.register(flow);
            engine.startFlow("flow");

            log.info("Started flow");
            log.info("8초 후 온도 급상승 시킴");

            Thread.sleep(8000);

            log.info("온도 급상승");
            simulator.setRegister(0, 320);
            Thread.sleep(6000);

            engine.shutdown();
            log.info("Stop Engine");
        } catch (Exception e) {
            log.error("시뮬레이션 비정상 작동: {}",e.getMessage());
        } finally {
            simulator.stop();
            log.info("Stop Simulator");
        }
    }
}
