package com.fbp.engine.bridge;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MqttBridgeConnection implements Connection {
    private final String id = UUID.randomUUID().toString();
    private final String topic;
    private final int qos;
    private final MessageSerializer serializer;
    private final BlockingQueue<Message> internalQueue;
    private IMqttClient mqttClient;

    @Getter
    @Setter
    private InputPort inputPort;

    @Getter
    @Setter
    private InputPort target;

    @Setter
    private BackPressureStrategy strategy;

    public MqttBridgeConnection(String brokerUrl, String topic, int qos) {
        this.topic = topic;
        this.qos = qos;
        this.serializer = new MessageSerializer();
        this.internalQueue = new LinkedBlockingQueue<>(100);

        initializeMqttClient(brokerUrl);
    }

    private void initializeMqttClient(String brokerUrl) {
        try {
            String clientId = "fbp-bridge-" + UUID.randomUUID().toString().substring(0, 8);
            this.mqttClient = new MqttClient(brokerUrl, clientId);

            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            this.mqttClient.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
                    log.error("[MqttBridge] 연결 끊김: {}", mqttDisconnectResponse.getException().getMessage());
                }

                @Override
                public void mqttErrorOccurred(MqttException e) {

                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    try {
                        Message msg = serializer.deserialize(mqttMessage.getPayload());
                        if (strategy != null) {
                            if (!internalQueue.offer(msg)) {
                                strategy.handleFull(internalQueue, msg);
                            }
                        } else {
                            internalQueue.put(msg);
                        }
                    } catch (Exception e) {
                        System.err.println("[MqttBridge] 메시지 처리 실패: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttToken iMqttToken) {

                }

                @Override
                public void connectComplete(boolean b, String s) {

                }

                @Override
                public void authPacketArrived(int i, MqttProperties mqttProperties) {

                }
            });

            this.mqttClient.connect(options);
            this.mqttClient.subscribe(this.topic, this.qos);
        } catch (MqttException e) {
            throw new RuntimeException("시스템 브로커 연결 실패: " + brokerUrl, e);
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void deliver(Message message) {
        try {
            byte[] payload = serializer.serialize(message);
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(this.qos);

            if (mqttClient.isConnected()) {
                mqttClient.publish(this.topic, mqttMessage);
            }
        } catch (MqttException e) {
            throw new RuntimeException("메시지 발행 실패. Topic: " + this.topic, e);
        }
    }

    @Override
    public Message poll() throws InterruptedException {
        return internalQueue.take();
    }

    @Override
    public int getBufferSize() {
        return internalQueue.size();
    }

    @Override
    public void push(Message message) {
        deliver(message);
    }

    @Override
    public void close() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(this.topic);
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (MqttException e) {
            log.error("[MqttBridge] 자원 정리 중 에러: {}", e.getMessage());
        }
    }
}
