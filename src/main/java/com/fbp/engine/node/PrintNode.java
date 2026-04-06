package com.fbp.engine.node;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.Node;
import com.fbp.engine.core.portImpl.InputPortImpl;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PrintNode implements Node {
    private final String id;
    private final InputPort inputPort = new InputPortImpl();

    public PrintNode(String id) {
        this.id = id;
    }

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

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Message msg = inputPort.read();
            if (msg != null) {
                process(msg);
            }
        }
    }
}
