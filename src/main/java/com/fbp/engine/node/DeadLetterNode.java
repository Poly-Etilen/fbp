package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class DeadLetterNode extends AbstractNode {
    @Getter
    private final List<Message> deadLetterMessages = Collections.synchronizedList(new ArrayList<>());

    public DeadLetterNode(String id) {
        super(id);
    }

    @Override
    protected void onProcess(Message message) {
        String originalNode = (String) message.getPayload().getOrDefault("error_node", "unknown");
        String errorMessage = (String) message.getPayload().getOrDefault("error_message", "No error info");
        log.error("[DeadLetter] 최종 처리 실패 메시지 입고 - 출처 노드: {}, 사유: {}", originalNode, errorMessage);
        deadLetterMessages.add(message);
    }

    public void clear() {
        deadLetterMessages.clear();
    }
}
