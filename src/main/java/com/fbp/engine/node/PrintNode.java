package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.impl.DefaultInputPort;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrintNode implements Node {
    private final String id;
    @Getter
    private final InputPort inputPort;

    public PrintNode(String id) {
        this.id = id;
        this.inputPort = new DefaultInputPort("in", this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {
        log.info("[{}] {}", id, message.getPayload());
    }
}
