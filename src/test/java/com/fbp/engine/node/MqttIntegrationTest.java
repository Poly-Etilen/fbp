package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.eclipse.paho.mqttv5.client.*;
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

@Tag("integration")
class MqttIntegrationTest {
    private static final String BROKER_URL = "tcp://localhost:1883";
    private MqttClient testClient;
    private AtomicReference<String> receivedPayload;
    private CountDownLatch messageLatch;

    @BeforeEach
    void setup() throws Exception {
        testClient = new MqttClient(BROKER_URL, "test-validator-client");
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        receivedPayload = new AtomicReference<>(null);

        testClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                receivedPayload.set(new String(mqttMessage.getPayload()));
                if (messageLatch != null) {
                    messageLatch.countDown();
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {}
            @Override
            public void mqttErrorOccurred(MqttException e) {}
            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {}
            @Override
            public void connectComplete(boolean b, String s) {}
            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {}
        });
        testClient.connect(options);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testClient != null && testClient.isConnected()) {
            testClient.disconnect();
            testClient.close();
        }
    }

    @Test
    @DisplayName("Subscriber → Publisher 파이프라인")
    void testSubscriberToPublisherPipeline() throws Exception {
        MqttSubscriberNode subNode = new MqttSubscriberNode("sub-node", Map.of(
                "brokerUrl", BROKER_URL,
                "clientId", "sub-client-1",
                "topic", "sensor/in"
        ));
        MqttPublisherNode pubNode = new MqttPublisherNode("pub-node", Map.of(
                "brokerUrl", BROKER_URL,
                "clientId", "pub-client-1",
                "topic", "sensor/out"
        ));
        TransformNode removeTopicNode = new TransformNode("remove-topic", msg -> {
            Map<String, Object> payload = new HashMap<>(msg.getPayload());
            payload.remove("topic");
            return new Message(payload);
        });

        Connection subToTransform = new LocalConnection();
        Connection transformToPub = new LocalConnection();

        subNode.getOutputPort("out").connect(subToTransform);
        removeTopicNode.getOutputPort("out").connect(transformToPub);

        subNode.initialize();
        removeTopicNode.initialize();
        pubNode.initialize();

        messageLatch = new CountDownLatch(1);
        testClient.subscribe("sensor/out", 1);
        Thread.sleep(500);

        testClient.publish("sensor/in", new MqttMessage("{\"temp\": 25.5}".getBytes()));

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 3000) {
            if (subToTransform.getBufferSize() > 0) {
                removeTopicNode.process(subToTransform.poll());
            }
            if (transformToPub.getBufferSize() > 0) {
                pubNode.process(transformToPub.poll());
                break;
            }
            Thread.sleep(50);
        }
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        assertTrue(received);
        assertTrue(receivedPayload.get().contains("25.5"));
        subNode.shutdown();
        pubNode.shutdown();
    }

    @Test
    @DisplayName("다중 토픽 구독")
    void testWildcardSubscription() throws Exception {
        MqttSubscriberNode subNode = new MqttSubscriberNode("sub-wildcard", Map.of(
                "brokerUrl", BROKER_URL, "clientId", "sub-client-wildcard", "topic", "sensor/+"
        ));
        Connection outConn = new LocalConnection();
        subNode.getOutputPort("out").connect(outConn);
        subNode.initialize();
        Thread.sleep(500);

        testClient.publish("sensor/temp", new MqttMessage("TEMP".getBytes()));
        testClient.publish("sensor/humi", new MqttMessage("HUMI".getBytes()));

        Thread.sleep(1000);
        assertEquals(2, outConn.getBufferSize());

        Message msg1 = outConn.poll();
        Message msg2 = outConn.poll();

        String topic1 = (String) msg1.getPayload().get("topic");
        String topic2 = (String) msg2.getPayload().get("topic");

        assertTrue(topic1.startsWith("sensor/") && topic2.startsWith("sensor/"));
        assertNotEquals(topic1, topic2);

        subNode.shutdown();
    }

    @Test
    @DisplayName("QoS 1 전달 보장")
    void testQos1Delivery() throws Exception {
        MqttSubscriberNode subNode = new MqttSubscriberNode("sub-qos1", Map.of(
                "brokerUrl", BROKER_URL, "clientId", "sub-client-qos", "topic", "test/qos", "qos", 1
        ));
        Connection outConn = new LocalConnection();
        subNode.getOutputPort("out").connect(outConn);
        subNode.initialize();
        Thread.sleep(500);

        MqttMessage qosMessage = new MqttMessage("QoS 1 Test".getBytes());
        qosMessage.setQos(1);
        testClient.publish("test/qos", qosMessage);

        Thread.sleep(1000);
        assertEquals(1, outConn.getBufferSize());

        subNode.shutdown();
    }

    @Test
    @DisplayName("재연결 테스트")
    void testReconnection() throws Exception {
        MqttSubscriberNode subNode = new MqttSubscriberNode("sub-reconnect", Map.of(
                "brokerUrl", BROKER_URL,
                "clientId", "sub-client-reconnect",
                "topic", "test/reconnect"
        ));
        Connection outConn = new LocalConnection();
        subNode.getOutputPort("out").connect(outConn);
        subNode.initialize();
        Thread.sleep(500);

        subNode.disconnect();

        subNode.connect();
        Thread.sleep(1000);
        assertTrue(subNode.isConnected());

        testClient.publish("test/reconnect", new MqttMessage("After Reconnect".getBytes()));
        Thread.sleep(1000);

        assertEquals(1, outConn.getBufferSize());

        subNode.shutdown();
    }
}
