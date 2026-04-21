package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MqttSubscriberNodeUnitTest {

    private MqttSubscriberNode node;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = new HashMap<>();
        config.put("brokerUrl", "tcp://localhost:1883");
        config.put("clientId", "test-unit-sub");
        config.put("topic", "test/topic");

        node = new MqttSubscriberNode("test-sub-node", config);
    }

    @Test
    @DisplayName("포트 구성")
    void testOutputPortConfiguration() {
        assertNotNull(node.getOutputPort("out"));
    }

    @Test
    @DisplayName("초기 상태")
    void testInitialConnectionState() {
        assertFalse(node.isConnected());
    }

    @Test
    @DisplayName("config 조회")
    void testConfigRetrieval() throws Exception{
        Object brokerUrl = node.getConfig("brokerUrl");

        assertEquals("tcp://localhost:1883", brokerUrl);
    }

    @Test
    @DisplayName("JSON -> Message 변환")
    void testJsonParsingSuccess() throws Exception {
        Method parseMethod = MqttSubscriberNode.class.getDeclaredMethod("parsePayload", String.class);
        parseMethod.setAccessible(true);

        String validJson = "{\"temperature\": 25.5, \"humidity\": 60}";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(node, validJson);

        assertNotNull(result);
        assertEquals(25.5, result.get("temperature"));
        assertEquals(60, result.get("humidity"));
    }

    @Test
    @DisplayName("JSON 파싱 실패 처리")
    void testJsonParsingFailure() throws Exception {
        Method parseMethod = MqttSubscriberNode.class.getDeclaredMethod("parsePayload", String.class);
        parseMethod.setAccessible(true);

        String invalidJson = "Invalid JSON String!";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(node, invalidJson);

        assertNotNull(result);
        assertEquals("Invalid JSON String!", result.get("rawPayload"));
    }

    @Nested
    @Tag("integration")
    class IntegrationTests {

        private MqttClient testPublisher;
        private CollectorNode collectorNode;
        private Connection connection;

        @BeforeEach
        void setUpIntegration() throws Exception {
            collectorNode = new CollectorNode("test-collector");
            connection = new Connection();
            node.getOutputPort("out").connect(connection);

            // MqttSubscriberNode 초기화 (Broker 연결)
            node.initialize();

            testPublisher = new MqttClient("tcp://localhost:1883", "test-pub-client", new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setCleanStart(true);
            testPublisher.connect(options);
        }

        @AfterEach
        void tearDownIntegration() throws Exception {
            if (testPublisher != null && testPublisher.isConnected()) {
                testPublisher.disconnect();
                testPublisher.close();
            }
            node.shutdown();
        }

        @Test
        @DisplayName("Broker 연결 성공")
        void testBrokerConnection() {
            assertTrue(node.isConnected());
        }

        @Test
        @DisplayName("메시지 수신")
        void testMessageReception() throws Exception {
            String payload = "{\"sensor\":\"temperature\", \"value\":22.5}";
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
            mqttMessage.setQos(1);
            testPublisher.publish("test/topic", mqttMessage);

            Message receivedMessage = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> connection.poll(), "지정된 시간 내에 메시지를 수신하지 못했습니다.");
            collectorNode.process(receivedMessage);

            assertEquals(1, collectorNode.getCollected().size());
        }

        @Test
        @DisplayName("토픽 정보 포함")
        void testTopicInformationIncluded() throws Exception {
            String payload = "{\"sensor\":\"humidity\", \"value\":60.0}";
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
            mqttMessage.setQos(1);
            testPublisher.publish("test/topic", mqttMessage);

            Message receivedMessage = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> connection.poll());
            collectorNode.process(receivedMessage);

            Message collectedMsg = collectorNode.getCollected().get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = collectedMsg.getPayload();

            assertTrue(body.containsKey("topic"));
            assertEquals("test/topic", body.get("topic"));
        }

        @Test
        @DisplayName("통합 9: shutdown 후 연결 해제")
        void testDisconnectOnShutdown() {
            node.shutdown();
            assertFalse(node.isConnected());
        }
    }
}