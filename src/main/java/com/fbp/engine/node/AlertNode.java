package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlertNode extends AbstractNode {
    public AlertNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        Object sensorId = message.get("sensorId");

        if (message.get("temperature") != null) {
            log.warn("[경고] 온도 센서 [{}] — {}°C (임계값 초과!)", sensorId, message.get("temperature"));
        } else if (message.get("humidity") != null) {
            log.warn("[경고] 습도 센서 [{}] — {}% (임계값 초과!)", sensorId, message.get("humidity"));
        } else {
            log.warn("[경고] 알 수 없는 센서 데이터: {}", message);
        }
    }
}
