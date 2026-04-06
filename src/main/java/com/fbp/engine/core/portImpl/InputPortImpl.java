package com.fbp.engine.core.portImpl;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InputPortImpl implements InputPort {
    private final Node node;

    @Override
    public void receive(Message message) {
        node.process(message);
    }
}
