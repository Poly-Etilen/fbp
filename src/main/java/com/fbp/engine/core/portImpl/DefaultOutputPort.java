package com.fbp.engine.core.portImpl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;

public class DefaultOutputPort implements OutputPort {
    private final String name;
    // OutputPort와 연결된 Connection을 모두 찾음
    private final List<Connection> connections = new ArrayList<>();

    public DefaultOutputPort(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void send(Message message) {
        // 연결된 Connection을 순회하며 모든 Connection에게 메시지를 보냄
        for (Connection conn : connections) {
            conn.deliver(message);
        }
    }

    @Override
    public void connect(Connection connection) {
        // OutputPort에 Connection을 추가함
        connections.add(connection);
    }
}
