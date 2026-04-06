package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GeneratorNode implements Node {
    private final String id;
    private final OutputPort output;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {

    }
}
