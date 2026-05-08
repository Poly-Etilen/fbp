package com.fbp.engine.core.port.impl;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorPort extends DefaultOutputPort{

    public ErrorPort(String name) {
        super(name);
    }

    public void sendError(Message originalMessage, String nodeId, Throwable throwable) {
        if (!isConnected()) {
            log.warn("[{}] 에러 포트가 연결되어 있지 않아 에러를 처리할 수 없습니다: {}", nodeId, throwable.getMessage());
            return;
        }

        Message errorMessage = originalMessage
                .withEntry("error_node", nodeId)
                .withEntry("error_msg", throwable.getMessage())
                .withEntry("error_type", throwable.getClass().getSimpleName())
                .withEntry("error_timestamp", System.currentTimeMillis());

        log.debug("[{}] 에러 포트를 통해 에러 메시지 전송 시작", nodeId);
        super.send(errorMessage);
    }
}
