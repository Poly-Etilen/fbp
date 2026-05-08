package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ErrorHandlerNode extends AbstractNode {
    private final int maxRetries;
    public static final String RETRY_PORT = "retry";
    public static final String FAILED_PORT = "failed";
    private static final String RETRY_COUNT_KEY = "retry_count";

    public ErrorHandlerNode(String id, int maxRetries) {
        super(id);
        this.maxRetries = maxRetries;
        addOutputPort(RETRY_PORT);
        addOutputPort(FAILED_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        Map<String, Object> payload = message.getPayload();
        String errorNode = (String)payload.get("error_node");
        String errorMsg = (String)payload.get("error_messsage");
        int currentRetry = (int) payload.getOrDefault(RETRY_COUNT_KEY, 0);

        log.error("[ErrorHandler] 노드 '{}'에서 발생한 에러 수신: {}, (재시도: {}/{})", errorNode, errorMsg, currentRetry, maxRetries);
        if (currentRetry < maxRetries) {
            Message retryMessage = message.withEntry(RETRY_COUNT_KEY, currentRetry + 1);
            log.info("[ErrorHandler] 노드 '{}'로 재시도 메시지를 전송합니다.", errorNode);
            send(RETRY_PORT, retryMessage);
        } else {
            log.warn("[ErrorHandler] 최대 재시도 횟수({})를 초과했습니다.", maxRetries);
            send(FAILED_PORT, message);
        }
    }
}
