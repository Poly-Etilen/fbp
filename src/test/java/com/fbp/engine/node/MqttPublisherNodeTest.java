package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MqttPublisherNodeTest {

    private MqttPublisherNode node;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = new HashMap<>();
        config.put("brokerUrl", "tcp://localhost:1883");
        config.put("clientId", "test-pub-node-client");
        config.put("topic", "default/topic");
        node = new MqttPublisherNode("pubNode", config);
    }

    // ==========================================
    // 단위 테스트 (Broker 불필요)
    // ==========================================
    @Test
    @DisplayName("단위 1: 포트 구성 - getInputPort(\"in\")이 null이 아님")
    void testInputPortConfigured() {
        assertNotNull(node.getInputPort("in"));
    }

    @Test
    @DisplayName("단위 2: 초기 상태 - 생성 직후 isConnected()가 false")
    void testInitialState() {
        assertFalse(node.isConnected());
    }

    @Test
    @DisplayName("단위 3: config 기본 토픽 조회 - getConfig(\"topic\")가 설정 값과 일치")
    void testConfigDefaultTopic() {
        assertEquals("default/topic", config.get("topic"));
    }


    // ==========================================
    // 통합 테스트 (Broker 필요)
    // ==========================================
    @Nested
    @Tag("integration")
    class IntegrationTests {

        private MqttClient testSubscriber;

        @BeforeEach
        void setUpIntegration() throws Exception {
            node.initialize(); // 노드 Broker 연결 시작
            
            // 검증을 위한 테스트용 Subscriber 연결
            testSubscriber = new MqttClient("tcp://localhost:1883", "test-sub-client", new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setCleanStart(true);
            testSubscriber.connect(options);
        }

        @AfterEach
        void tearDownIntegration() throws Exception {
            if (testSubscriber != null && testSubscriber.isConnected()) {
                testSubscriber.disconnect();
                testSubscriber.close();
            }
            node.shutdown();
        }

        @Test
        @DisplayName("통합 4: Broker 연결 성공 - initialize() 후 isConnected()가 true")
        void testBrokerConnection() {
            assertTrue(node.isConnected());
        }

        @Test
        @DisplayName("통합 5 & 6: 메시지 발행 및 동적 토픽 동작 검증")
        void testPublishingAndDynamicTopic() throws Exception {
            // CountDownLatch를 이용해 메시지 수신 대기
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> receivedTopic = new AtomicReference<>();
            AtomicReference<String> receivedPayload = new AtomicReference<>();

            testSubscriber.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    receivedTopic.set(topic);
                    receivedPayload.set(new String(message.getPayload()));
                    latch.countDown();
                }
                @Override public void disconnected(MqttDisconnectResponse r) {}
                @Override public void mqttErrorOccurred(MqttException e) {}
                @Override public void deliveryComplete(IMqttToken t) {}
                @Override public void connectComplete(boolean b, String s) {}
                @Override public void authPacketArrived(int i, MqttProperties p) {}
            });

            // "dynamic/topic"으로 구독 시작
            testSubscriber.subscribe("dynamic/topic", 1);

            // FBP Message 생성 및 "topic" 동적 지정
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("topic", "dynamic/topic"); // 통합 6 (동적 토픽 테스트)
            payloadMap.put("data", "Hello FBP");
            Message fbpMessage = new Message(payloadMap);

            // when
            node.process(fbpMessage);

            // then
            boolean isReceived = latch.await(3, TimeUnit.SECONDS); // 3초 대기
            assertTrue(isReceived, "지정된 시간 내에 메시지가 발행/수신되지 않았습니다.");
            
            assertEquals("dynamic/topic", receivedTopic.get());
            assertTrue(receivedPayload.get().contains("Hello FBP"));
        }

        @Test
        @DisplayName("통합 7: shutdown 후 연결 해제")
        void testDisconnectOnShutdown() {
            node.shutdown();
            assertFalse(node.isConnected());
        }
    }
}