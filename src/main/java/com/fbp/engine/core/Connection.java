package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import lombok.Setter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class Connection {
    private final String id = UUID.randomUUID().toString();
    private final Queue<Message> buffer = new LinkedList<>();
    @Setter
    private InputPort target;

    public void deliver(Message message) {
        buffer.add(message);
        if (target != null) {
            target.receive(buffer.poll());
        }
    }

    public int getBufferSize() {
        return buffer.size();
    }
}
