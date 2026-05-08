package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;

public class DefaultOutputPort implements OutputPort {
    private final String name;
    private final List<Connection> connections = new ArrayList<>();

    public DefaultOutputPort(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void connect(Connection connection) {
        connections.add(connection);
    }

    @Override
    public void send(Message message) {
        for (Connection conn : connections) {
//            conn.deliver(message);
            conn.push(message);
        }
    }

    @Override
    public boolean isConnected() {
        return !connections.isEmpty();
    }

    @Override
    public void disconnect(Connection connection) {
        this.connections.remove(connection);
    }

    @Override
    public void disconnectAll() {
        this.connections.clear();
    }
}
