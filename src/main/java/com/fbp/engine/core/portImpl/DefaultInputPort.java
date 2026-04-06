package com.fbp.engine.core.portImpl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultInputPort implements InputPort {
    private final String name;
    private final Node owner;
    private Connection connection;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Message read() {
        if (connection == null) {
            return null;
        }
        return connection.poll();
    }

    @Override
    public void connect(Connection connection) {
        this.connection = connection;
    }
}
