package com.fbp.engine.practice;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

@Slf4j
public class MqttPractice {
    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:1883";
        String clientId = "client-id";
        String topic = "sensor/temperature";

        try {
            MqttClient client = new MqttClient(brokerUrl, clientId);
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            client.connect(options);
            log.info("MQTT broker 연결 성공");

            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String t, MqttMessage msg) throws Exception {
                    // 수신된 메시지는 모두 이쪽으로 들어옵니다.
                    String payload = new String(msg.getPayload());
                    log.info("수신된 토픽: {}", t);
                    log.info("수신된 메시지: {}", payload);
                }

                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    log.warn("연결 끊어짐");
                }
                @Override
                public void mqttErrorOccurred(MqttException exception) {
                    log.error("MQTT 에러", exception);
                }
                @Override public void deliveryComplete(IMqttToken token) {}
                @Override public void connectComplete(boolean reconnect, String serverURI) {}
                @Override public void authPacketArrived(int reasonCode, MqttProperties properties) {}
            });
            client.subscribe(topic, 1);

            MqttMessage message = new MqttMessage("25.5".getBytes());
            message.setQos(1);

            log.info("메시지 발행 중");
            client.publish(topic, message);

            Thread.sleep(2000);

            client.disconnect();
            client.close();
            log.info("연결 종료");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
