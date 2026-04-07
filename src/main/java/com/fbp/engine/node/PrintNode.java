package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PrintNode implements Node {
    private final String id;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {
        log.info("[{}] {}", id, message.getPayload());
    }
}
