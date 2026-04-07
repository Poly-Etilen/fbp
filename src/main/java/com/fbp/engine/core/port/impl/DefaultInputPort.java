package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultInputPort implements InputPort {
    private final String name;
    private final Node owner;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void receive(Message message) {
        owner.process(message);
    }
}
