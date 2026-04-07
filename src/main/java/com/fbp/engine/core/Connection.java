package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection {
    private final String id = UUID.randomUUID().toString();
    private final BlockingQueue<Message> buffer;
    private InputPort inputPort;

    public Connection() {
        this(100);
    }
    public Connection(int capacity) {
        this.buffer = new LinkedBlockingQueue<>(capacity);
    }
    @Setter
    private InputPort target;

    public void deliver(Message message) {
        try {
            buffer.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Message poll() throws InterruptedException {
        return buffer.take();
    }

    public int getBufferSize() {
        return buffer.size();
    }
}
