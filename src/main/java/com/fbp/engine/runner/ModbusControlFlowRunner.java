package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.node.*;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ModbusControlFlowRunner {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();
        simulator.setRegister(0, 60);
        simulator.setRegister(2, 0);
        log.info("초기 시뮬레이터 레지스터 상태 - [주소 0(온도): {}], [주소 2(제어): {}]", 
                simulator.getRegister(0), simulator.getRegister(2));

        try {
            TimerNode timer = new TimerNode("timer", 2000L); // 2초 주기
            Map<String, Object> readerConfig = Map.of(
                    "host", "localhost", "port", 5020, "slaveId", 1,
                    "startAddress", 0, "count", 1,
                    "registerMapping", Map.of("0", Map.of("name", "temperature", "scale", 1.0))
            );
            ModbusReaderNode reader = new ModbusReaderNode("reader", readerConfig);

            TransformNode mapper = new TransformNode("mapper", msg -> {
                Map<String, Object> data = msg.get("data");
                if (data != null && data.containsKey("temperature")) {
                    double temp = ((Number) data.get("temperature")).doubleValue();
                    return msg.withEntry("temperature", temp).withEntry("alertCode", 1);
                }
                return msg;
            });

            ThresholdFilterNode filter = new ThresholdFilterNode("filter", "temperature", 50.0);
            Map<String, Object> writerConfig = Map.of(
                    "host", "localhost", "port", 5020, "slaveId", 1,
                    "registerAddress", 2, "valueField", "alertCode"
            );
            ModbusWriterNode writer = new ModbusWriterNode("writer", writerConfig);

            Flow flow = new Flow("auto-control-flow", "자동 제어 플로우");
            flow.addNode(timer).addNode(reader).addNode(mapper).addNode(filter).addNode(writer);
            flow.connect("timer", "out", "reader", "trigger")
                .connect("reader", "out", "mapper", "in")
                .connect("mapper", "out", "filter", "in")
                .connect("filter", "alert", "writer", "in"); // ★ 핵심: 임계값 초과(alert) 시에만 쓰기 수행

            FlowEngine engine = new FlowEngine();
            engine.register(flow);
            engine.startFlow("auto-control-flow");

            log.info("플로우 시작. 데이터 처리 및 제어 신호 전송을 위해 5초 대기...");
            Thread.sleep(5000);

            int finalAlertCode = simulator.getRegister(2);
            log.info("=== 5초 후 시뮬레이터 확인 ===");
            log.info("2번 주소(제어 레지스터)의 현재 값: {}", finalAlertCode);
            if (finalAlertCode == 1) {
                log.info("결과: 정상 작동! 초과 온도를 감지하여 제어 코드가 성공적으로 기록되었습니다.");
            } else {
                log.warn("결과: 제어 코드가 기록되지 않았습니다.");
            }

            engine.shutdown();
        } catch (Exception e) {
            log.error("플로우 실행 중 오류: {}", e.getMessage(), e);
        } finally {
            simulator.stop();
        }
    }
}