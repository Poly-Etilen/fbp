package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

// stage2. 5-2
@Slf4j
public class ErrorSimulationRunner {

    public static void main(String[] args) throws Exception {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();

        MqttClient testClient = new MqttClient("tcp://localhost:1883", "error-injector");
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        testClient.connect(options);

        MqttSubscriberNode subNode = new MqttSubscriberNode("mqtt-sub", Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "sim-sub",
                "topic", "sensor/temp"
        ));

        TimerNode timerNode = new TimerNode("timer", 2000);

        ModbusReaderNode readerNode = new ModbusReaderNode("modbus-in", Map.of(
                "host", "localhost",
                "port", 5020,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        ));

        LogNode errorLogNode = new LogNode("log-error-dlq");
        LogNode normalLogNode = new LogNode("log-normal");

        Connection subToLog = new Connection();
        Connection timerToReader = new Connection();
        Connection readerToNormalLog = new Connection();
        Connection readerToErrorLog = new Connection();

        subNode.getOutputPort("out").connect(subToLog);
        timerNode.getOutputPort("out").connect(timerToReader);
        readerNode.getOutputPort("out").connect(readerToNormalLog);
        readerNode.getOutputPort("error").connect(readerToErrorLog);

        subNode.initialize();
        timerNode.initialize();
        readerNode.initialize();
        normalLogNode.initialize();
        errorLogNode.initialize();

        Thread.sleep(1000);

        // 시뮬레이션 1: 잘못된 JSON 형태의 데이터 발행
        log.warn("[시뮬레이션 1] 잘못된 포맷의 JSON 데이터(문자열)를 MQTT로 발행");
        testClient.publish("sensor/temp", new MqttMessage("THIS_IS_NOT_JSON".getBytes()));
        
        Thread.sleep(1000);
        if (subToLog.getBufferSize() > 0) {
            log.info("(결과) 잘못된 JSON이지만 예외로 죽지 않고 정상적으로 다음 노드로 전달됨.");
            normalLogNode.process(subToLog.poll());
        }

        // 시뮬레이션 2: MODBUS 장비 강제 정지 (응답 없음 타임아웃)
        log.warn("[시뮬레이션 2] MODBUS 시뮬레이터를 강제 종료합니다. (네트워크 단절 상황)");
        simulator.stop();

        log.info("(타임아웃 3초 대기 중...)");
        int retryCount = 0;
        while (retryCount < 3) {
            if (timerToReader.getBufferSize() > 0) {
                readerNode.process(timerToReader.poll()); 
                retryCount++;
            }
            if (readerToErrorLog.getBufferSize() > 0) {
                log.error("(결과) ModbusReader가 예외를 발생시켜 'error' 포트로 배출함");
                errorLogNode.process(readerToErrorLog.poll());
            }
            Thread.sleep(100);
        }

        log.info("=== 시뮬레이션 종료 ===");
        System.exit(0);
    }
}