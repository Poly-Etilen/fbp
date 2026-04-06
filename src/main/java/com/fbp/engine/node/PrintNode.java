package com.fbp.engine.node;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.portImpl.InputPortImpl;
import com.fbp.engine.message.Message;
import com.fbp.engine.core.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PrintNode implements Node {
    private final String id;
    private InputPort inputPort = new InputPortImpl(null);

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {
        log.info("Node: {}", id);
        log.info("Message: {}", message.getId());
        log.info("Payload: {}", message.getPayload());
        log.info("Timestamp: {}", message.getTimestamp());
    }
}
