package com.fbp.engine.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MqttSubscriberNode extends ProtocolNode{
    private MqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqttSubscriberNode(String id, Map<String, Object> config) {
        super(id, config);
        addOutputPort("out");
    }

    @Override
    protected void connect() throws Exception {
        if (client != null && client.isConnected()) {
            log.info("[{}] 이미 연결됨", getId());
            return;
        }

        String brokerUrl = (String) config.getOrDefault("brokerUrl", "tcp://localhost:1883");
        String clientId = (String) config.getOrDefault("clientId", "sub-" + getId());
        String topic = (String) config.getOrDefault("topic", "sensor/#");
        int qos = (int) config.getOrDefault("qos", 1);

        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        client.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String payloadStr = new String(mqttMessage.getPayload());
                Map<String, Object> payloadMap = parsePayload(payloadStr);

                payloadMap.put("topic", s);
                payloadMap.put("mqttTimestamp", System.currentTimeMillis() );
                send("out", new Message(payloadMap));
                log.debug("[{}] MQTT 메시지 수신 및 플로우 전달: {}", getId(), s);
            }

            @Override
            public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
                log.warn("[{}] MQTT 연결 끊김", getId());
            }

            @Override
            public void mqttErrorOccurred(MqttException e) {
                log.error("[{}] MQTT 에러 발생", getId(), e);
            }

            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {

            }

            @Override
            public void connectComplete(boolean b, String s) {
                log.info("[{}] MQTT 연결 완료 (reconnect: {})", getId(), b);
            }

            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {

            }
        });

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        client.connect(options);

        client.subscribe(topic, qos);
        log.info("[{}] MQTT Broker 연결됨: {}", getId(), brokerUrl);
    }

    @Override
    protected void disconnect() {
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            }
        } catch (Exception e) {
            log.error("[{}] MQTT 연결 해제 중 오류: {}", getId(), e.getMessage());
        }
    }

    @Override
    protected void onProcess(Message message) {

    }

    protected Map<String, Object> parsePayload(String payloadStr) {
        Map<String, Object> payloadMap;
        try {
            payloadMap = objectMapper.readValue(payloadStr, Map.class);
        } catch (Exception e) {
            payloadMap = new HashMap<>();
            payloadMap.put("rawPayload", payloadStr);
        }
        return payloadMap;
    }
}
