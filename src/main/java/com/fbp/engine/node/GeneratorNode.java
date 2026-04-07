package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import lombok.Getter;

import java.util.Map;

public class GeneratorNode implements Node {
    private final String id;
    @Getter
    private final OutputPort outputPort;

    public GeneratorNode(String id) {
        this.id = id;
        this.outputPort = new DefaultOutputPort("out");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {}

    public void generate(String key, Object value) {
        Message msg = new Message(Map.of(key, value));
        outputPort.send(msg);
    }
}
