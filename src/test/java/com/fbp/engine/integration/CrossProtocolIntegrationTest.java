package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
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
class CrossProtocolIntegrationTest {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final int MODBUS_PORT = 5025;

    private MqttClient testClient;
    private ModbusTcpSimulator simulator;
    private AtomicReference<String> receivedPayload;
    private CountDownLatch messageLatch;

    @BeforeEach
    void setUp() throws Exception {
        simulator = new ModbusTcpSimulator(MODBUS_PORT, 20);
        simulator.start();

        testClient = new MqttClient(BROKER_URL, "cross-proto-tester");
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        receivedPayload = new AtomicReference<>(null);

        testClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                receivedPayload.set(new String(message.getPayload()));
                if (messageLatch != null) {
                    messageLatch.countDown();
                }
            }
            @Override
            public void disconnected(MqttDisconnectResponse response) {}
            @Override
            public void mqttErrorOccurred(MqttException exception) {}
            @Override
            public void deliveryComplete(IMqttToken token) {}
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}
            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        testClient.connect(options);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testClient != null && testClient.isConnected()) {
            testClient.disconnect();
            testClient.close();
        }
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("MQTT → Rule → MODBUS")
    void testMqttToModbusFlow() throws Exception {
        MqttSubscriberNode subNode = new MqttSubscriberNode("sub", Map.of(
                "brokerUrl", BROKER_URL,
                "clientId", "sub-cross-1",
                "topic", "sensor/temp"
        ));

        RuleNode ruleNode = new RuleNode("rule", msg -> {
            Number temp = (Number) msg.getPayload().get("temperature");
            return temp != null && temp.doubleValue() > 30.0;
        });

        TransformNode transformNode = new TransformNode("transform", msg -> {
            Map<String, Object> newPayload = new HashMap<>(msg.getPayload());
            newPayload.put("fanControl", 1);
            return new Message(newPayload);
        });

        ModbusWriterNode writerNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", MODBUS_PORT,
                "slaveId", 1,
                "registerAddress", 5,
                "valueField", "fanControl",
                "scale", 1.0
        ));

        Connection subToRule = new LocalConnection();
        Connection ruleToTransform = new LocalConnection();
        Connection transformToWriter = new LocalConnection();

        subNode.getOutputPort("out").connect(subToRule);
        ruleNode.getOutputPort("match").connect(ruleToTransform);
        transformNode.getOutputPort("out").connect(transformToWriter);

        subNode.initialize();
        ruleNode.initialize();
        transformNode.initialize();
        writerNode.initialize();
        Thread.sleep(500);

        testClient.publish("sensor/temp", new MqttMessage("{\"temperature\": 35.5}".getBytes()));

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 2000) {
            if (subToRule.getBufferSize() > 0) {
                ruleNode.process(subToRule.poll());
            }
            if (ruleToTransform.getBufferSize() > 0) {
                transformNode.process(ruleToTransform.poll());
            }
            if (transformToWriter.getBufferSize() > 0) {
                writerNode.process(transformToWriter.poll());
                break;
            }
            Thread.sleep(50);
        }
        Thread.sleep(200);

        assertEquals(1, simulator.getRegister(5));

        subNode.shutdown();
        writerNode.shutdown();
    }

    @Test
    @DisplayName("MODBUS → Rule → MQTT")
    void testModbusToMqttFlow() throws Exception {
        simulator.setRegister(0, 450);

        ModbusReaderNode readerNode = new ModbusReaderNode("reader", Map.of(
                "host", "localhost",
                "port", MODBUS_PORT,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        ));

        TransformNode parseNode = new TransformNode("parse", msg -> {
            int[] regs = (int[]) msg.getPayload().get("registers");
            return new Message(Map.of("temp", regs[0] / 10.0));
        });

        RuleNode ruleNode = new RuleNode("rule", msg -> {
            Double temp = (Double) msg.getPayload().get("temp");
            return temp != null && temp > 40.0;
        });

        MqttPublisherNode pubNode = new MqttPublisherNode("pub", Map.of(
                "brokerUrl", BROKER_URL,
                "clientId", "pub-cross-2",
                "topic", "alert/high_temp"
        ));

        Connection readerToParse = new LocalConnection();
        Connection parseToRule = new LocalConnection();
        Connection ruleToPub = new LocalConnection();

        readerNode.getOutputPort("out").connect(readerToParse);
        parseNode.getOutputPort("out").connect(parseToRule);
        ruleNode.getOutputPort("match").connect(ruleToPub);

        readerNode.initialize();
        parseNode.initialize();
        ruleNode.initialize();
        pubNode.initialize();

        messageLatch = new CountDownLatch(1);
        testClient.subscribe("alert/high_temp", 1);
        Thread.sleep(500);

        readerNode.process(new Message(Map.of()));

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 2000) {
            if (readerToParse.getBufferSize() > 0) parseNode.process(readerToParse.poll());
            if (parseToRule.getBufferSize() > 0) ruleNode.process(parseToRule.poll());
            if (ruleToPub.getBufferSize() > 0) {
                pubNode.process(ruleToPub.poll());
                break;
            }
            Thread.sleep(50);
        }

        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        assertTrue(received);
        assertTrue(receivedPayload.get().contains("45.0"));

        readerNode.shutdown();
        pubNode.shutdown();
    }

    @Test
    @DisplayName("복합 플로우 안정성")
    void testComplexFlowStability() throws Exception {
        MqttSubscriberNode subNode = new MqttSubscriberNode("sub", Map.of(
                "brokerUrl", BROKER_URL,
                "clientId", "sub-cross-stable",
                "topic", "stress/in"
        ));
        RuleNode ruleNode = new RuleNode("rule", msg -> true);
        TransformNode transformNode = new TransformNode("transform", msg -> {
            Map<String, Object> payload = new HashMap<>(msg.getPayload());
            payload.put("writeValue", 99); 
            return new Message(payload);
        });
        ModbusWriterNode writerNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", MODBUS_PORT,
                "slaveId", 1,
                "registerAddress", 10,
                "valueField", "writeValue",
                "scale", 1.0
        ));

        Connection subToRule = new LocalConnection();
        Connection ruleToTrans = new LocalConnection();
        Connection transToWriter = new LocalConnection();

        subNode.getOutputPort("out").connect(subToRule);
        ruleNode.getOutputPort("match").connect(ruleToTrans);
        transformNode.getOutputPort("out").connect(transToWriter);

        subNode.initialize();
        ruleNode.initialize();
        transformNode.initialize();
        writerNode.initialize();
        Thread.sleep(500);

        long endTime = System.currentTimeMillis() + 3000;
        int pumpCount = 0;

        while (System.currentTimeMillis() < endTime) {
            if (pumpCount % 5 == 0) {
                testClient.publish("stress/in", new MqttMessage("{\"stress\": 1}".getBytes()));
            }

            if (subToRule.getBufferSize() > 0) ruleNode.process(subToRule.poll());
            if (ruleToTrans.getBufferSize() > 0) transformNode.process(ruleToTrans.poll());
            if (transToWriter.getBufferSize() > 0) writerNode.process(transToWriter.poll());

            pumpCount++;
            Thread.sleep(10);
        }
        assertEquals(99, simulator.getRegister(10));

        subNode.shutdown();
        writerNode.shutdown();
    }
}