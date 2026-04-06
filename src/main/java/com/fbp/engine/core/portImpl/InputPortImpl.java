package com.fbp.engine.core.portImpl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.InputPort;
import com.fbp.engine.message.Message;


public class InputPortImpl implements InputPort {
    private Connection connection;

    @Override
    public Message read() {
        if (connection == null) {
            return null;
        }
        return connection.receive();
    }

    @Override
    public void connect(Connection connection) {
        this.connection = connection;
    }
}
