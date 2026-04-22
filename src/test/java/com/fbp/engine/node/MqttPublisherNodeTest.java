package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
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

    @Test
    @DisplayName("포트 구성")
    void testInputPortConfigured() {
        assertNotNull(node.getInputPort("in"));
    }

    @Test
    @DisplayName("초기 상태")
    void testInitialState() {
        assertFalse(node.isConnected());
    }

    @Test
    @DisplayName("config 기본 토픽 조회")
    void testConfigDefaultTopic() {
        assertEquals("default/topic", config.get("topic"));
    }


    @Nested
    @Tag("integration")
    class IntegrationTests {

        private MqttClient testSubscriber;

        @BeforeEach
        void setUpIntegration() throws Exception {
            node.initialize();
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
        @DisplayName("Broker 연결 성공")
        void testBrokerConnection() {
            assertTrue(node.isConnected());
        }

        @Test
        @DisplayName("메시지 발행")
        void testMessagePublishing() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> receivedPayload = new AtomicReference<>();

            testSubscriber.setCallback(new SimpleMqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    receivedPayload.set(new String(message.getPayload()));
                    latch.countDown();
                }
            });

            testSubscriber.subscribe("default/topic", 1);

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("data", "Hello World");
            Message fbpMessage = new Message(payloadMap);

            node.process(fbpMessage);

            boolean isReceived = latch.await(3, TimeUnit.SECONDS);
            assertTrue(isReceived);
            assertTrue(receivedPayload.get().contains("Hello World"));
        }

        @Test
        @DisplayName("동적 토픽")
        void testDynamicTopicPublishing() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> receivedTopic = new AtomicReference<>();
            AtomicReference<String> receivedPayload = new AtomicReference<>();

            testSubscriber.setCallback(new SimpleMqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    receivedTopic.set(topic);
                    receivedPayload.set(new String(message.getPayload()));
                    latch.countDown();
                }
            });

            testSubscriber.subscribe("dynamic/topic", 1);
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("topic", "dynamic/topic");
            payloadMap.put("data", "Hello World");
            Message fbpMessage = new Message(payloadMap);

            node.process(fbpMessage);
            boolean isReceived = latch.await(3, TimeUnit.SECONDS);
            assertTrue(isReceived);
            assertEquals("dynamic/topic", receivedTopic.get());
            assertTrue(receivedPayload.get().contains("Hello World"));
        }

        @Test
        @DisplayName("shutdown 후 연결 해제")
        void testDisconnectOnShutdown() {
            node.shutdown();
            assertFalse(node.isConnected());
        }

        private abstract class SimpleMqttCallback implements MqttCallback {
            @Override public void disconnected(MqttDisconnectResponse r) {}
            @Override public void mqttErrorOccurred(MqttException e) {}
            @Override public void deliveryComplete(IMqttToken t) {}
            @Override public void connectComplete(boolean b, String s) {}
            @Override public void authPacketArrived(int i, MqttProperties p) {}
        }
    }
}