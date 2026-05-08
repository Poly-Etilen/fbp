package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.MqttPublisherNode;
import com.fbp.engine.node.MqttSubscriberNode;
import com.fbp.engine.node.RuleNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class MqttModbusIntegrationTest {

    private ModbusTcpSimulator modbusSimulator;
    private MqttClient testMqttClient;

    // 테스트 대상 노드들
    private MqttSubscriberNode subscriberNode;
    private RuleNode ruleNode;
    private ModbusWriterNode writerNode;
    private MqttPublisherNode publisherNode;

    // 노드 간 연결 파이프
    private Connection subToRuleConn;
    private Connection ruleToModbusConn;
    private Connection ruleToMqttConn;
    private Connection ruleMismatchConn;

    // 검증용 알림 수신 저장소
    private AtomicReference<String> receivedAlertPayload;

    @BeforeEach
    void setUp() throws Exception {
        modbusSimulator = new ModbusTcpSimulator(5020, 10);
        modbusSimulator.start();

        testMqttClient = new MqttClient("tcp://localhost:1883", "test-injector-client");
        receivedAlertPayload = new AtomicReference<>(null);
        testMqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // 수신된 토픽이 "test/alert"인 경우만 데이터 저장
                if ("test/alert".equals(topic)) {
                    receivedAlertPayload.set(new String(message.getPayload()));
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {}

            @Override
            public void mqttErrorOccurred(MqttException exception) {}

            @Override
            public void deliveryComplete(IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        testMqttClient.connect(options);

        testMqttClient.subscribe("test/alert", 1);

        // 3. 노드 초기화 및 설정
        Map<String, Object> subConfig = Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "fbp-sub-node",
                "topic", "test/sensor",
                "qos", 1
        );
        subscriberNode = new MqttSubscriberNode("mqtt-sub", subConfig);

        ruleNode = new RuleNode("rule-node", msg -> {
            // temperature가 30 초과인지 판별
            Number temp = (Number) msg.getPayload().get("temperature");
            return temp != null && temp.doubleValue() > 30.0;
        });

        Map<String, Object> modbusConfig = Map.of(
                "host", "localhost",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature",
                "scale", 1.0
        );
        writerNode = new ModbusWriterNode("modbus-writer", modbusConfig);

        Map<String, Object> pubConfig = Map.of(
                "brokerUrl", "tcp://localhost:1883",
                "clientId", "fbp-pub-node",
                "topic", "test/alert",
                "qos", 1
        );
        publisherNode = new MqttPublisherNode("mqtt-pub", pubConfig);

        subToRuleConn = new LocalConnection();
        ruleToModbusConn = new LocalConnection();
        ruleToMqttConn = new LocalConnection();
        ruleMismatchConn = new LocalConnection();

        subscriberNode.getOutputPort("out").connect(subToRuleConn);

        ruleNode.getOutputPort("match").connect(ruleToModbusConn);
        ruleNode.getOutputPort("match").connect(ruleToMqttConn);
        ruleNode.getOutputPort("mismatch").connect(ruleMismatchConn);

        subscriberNode.initialize();
        ruleNode.initialize();
        writerNode.initialize();
        publisherNode.initialize();

        // MQTT 연결 대기 시간
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() throws Exception {
        subscriberNode.shutdown();
        writerNode.shutdown();
        publisherNode.shutdown();

        if (testMqttClient != null && testMqttClient.isConnected()) {
            testMqttClient.disconnect();
            testMqttClient.close();
        }
        if (modbusSimulator != null) {
            modbusSimulator.stop();
        }
    }

    /**
     * 비동기 메시지 수신 대기를 위한 헬퍼 메서드
     */
    private boolean waitForMessage(Connection connection, int timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (connection.getBufferSize() == 0) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return false;
            }
            Thread.sleep(50);
        }
        return true;
    }

    @Test
    @DisplayName("MQTT 수신 → Rule 분기")
    void testMqttReceiveAndRuleBranching() throws Exception {
        String payload = "{\"temperature\": 35.0}";
        testMqttClient.publish("test/sensor", new MqttMessage(payload.getBytes()));

        assertTrue(waitForMessage(subToRuleConn, 2000));
        Message receivedMsg = subToRuleConn.poll();
        ruleNode.process(receivedMsg);

        assertEquals(1, ruleToModbusConn.getBufferSize());
        assertEquals(1, ruleToMqttConn.getBufferSize());
        assertEquals(0, ruleMismatchConn.getBufferSize());
    }

    @Test
    @DisplayName("Rule match → MODBUS 쓰기")
    void testRuleMatchToModbusWrite() throws Exception {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("temperature", 35.0);
        Message matchMsg = new Message(payloadMap);

        writerNode.process(matchMsg);

        // 딜레이 고려하여 잠시 대기
        Thread.sleep(100);
        assertEquals(35, modbusSimulator.getRegister(2));
    }

    @Test
    @DisplayName("Rule match → MQTT 알림")
    void testRuleMatchToMqttAlert() throws Exception {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("temperature", 35.0);
        payloadMap.put("alert", "High Temperature!");
        Message matchMsg = new Message(payloadMap);

        publisherNode.process(matchMsg);

        long startTime = System.currentTimeMillis();
        while (receivedAlertPayload.get() == null && System.currentTimeMillis() - startTime < 2000) {
            Thread.sleep(50);
        }

        assertNotNull(receivedAlertPayload.get());
        assertTrue(receivedAlertPayload.get().contains("35.0"));
    }

    @Test
    @DisplayName("End-to-End 흐름")
    void testEndToEndPipeline() throws Exception {
        String highTempPayload = "{\"temperature\": 40.0}";
        testMqttClient.publish("test/sensor", new MqttMessage(highTempPayload.getBytes()));

        assertTrue(waitForMessage(subToRuleConn, 2000));
        Message msg1 = subToRuleConn.poll();

        // Rule 평가
        ruleNode.process(msg1);

        // Match 분기 파이프라인 실행
        if (ruleToModbusConn.getBufferSize() > 0 && ruleToMqttConn.getBufferSize() > 0) {
            Message modbusMsg = ruleToModbusConn.poll();
            Message pubMsg = ruleToMqttConn.poll();
            Message modifiedPubMsg;

            Map<String, Object> copiedPayload = new HashMap<>(pubMsg.getPayload());
            copiedPayload.remove("topic");
            modifiedPubMsg = new Message(copiedPayload);

            writerNode.process(modbusMsg);
            publisherNode.process(modifiedPubMsg);
        } else {
            fail("메시지가 Match 포트로 분기되지 않았습니다.");
        }

        Thread.sleep(200);
        assertEquals(40, modbusSimulator.getRegister(2), "E2E: Modbus에 40이 기록되어야 합니다.");
        assertNotNull(receivedAlertPayload.get());
        assertTrue(receivedAlertPayload.get().contains("40.0"));

        // 상태 초기화
        receivedAlertPayload.set(null);

        String normalTempPayload = "{\"temperature\": 20.0}";
        testMqttClient.publish("test/sensor", new MqttMessage(normalTempPayload.getBytes()));
        assertTrue(waitForMessage(subToRuleConn, 2000));
        Message msg2 = subToRuleConn.poll();

        ruleNode.process(msg2);

        assertEquals(0, ruleToModbusConn.getBufferSize());
        assertEquals(1, ruleMismatchConn.getBufferSize());

        Thread.sleep(200);
        assertEquals(40, modbusSimulator.getRegister(2));
        assertNull(receivedAlertPayload.get());
    }
}