package com.fbp.engine.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.Map;

@Slf4j
public class MqttPublisherNode extends ProtocolNode{
    private MqttClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public MqttPublisherNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("in");
    }

    @Override
    protected void connect() throws Exception {
        if (client != null && client.isConnected()) {
            log.info("[{}] 이미 연결되어 있습니다. 중복 연결 무시.", getId());
            return;
        }

        String brokerUrl = (String) config.getOrDefault("brokerUrl", "tcp://localhost:1883");
        String clientId = (String) config.getOrDefault("clientId", "pub-" + getId());
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        client.setCallback(new MqttCallback() {

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

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
                log.trace("[{}] 메시지 프로커 전달 완료", getId());
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
        log.info("[{}] MQTT Broker 준비 완료: {}", getId(), brokerUrl);
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
        if (!isConnected()) {
            log.warn("[{}] 연결 끊김 상태, 발행 무시됨.", getId());
            return;
        }
        try {
            String targetTopic = (String) config.get("topic");
            if (targetTopic == null || targetTopic.trim().isEmpty()) {
                targetTopic = message.getPayload().containsKey("topic")
                        ? (String) message.getPayload().get("topic")
                        : "default/topic"; // 둘 다 없으면 기본값 사용
            }

            String jsonPayload = mapper.writeValueAsString(message.getPayload());
            MqttMessage mqttMessage = new MqttMessage(jsonPayload.getBytes());
            mqttMessage.setQos((int) config.getOrDefault("qos", 1));
            mqttMessage.setRetained((boolean) config.getOrDefault("retained", false));

            client.publish(targetTopic, mqttMessage);
            log.debug("[{}] 외부 브로커로 메시지 발행 완료 [토픽: {}]", getId(), targetTopic);
        } catch (Exception e) {
            log.error("[{}] 메시지 발행 중 오류 발생 : {}",  getId(), e.getMessage());
        }
    }
}
