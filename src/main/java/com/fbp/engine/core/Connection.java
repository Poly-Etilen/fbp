package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection {
    private final String id = UUID.randomUUID().toString();
    private final BlockingQueue<Message> buffer;
    @Getter
    @Setter
    private InputPort inputPort;

    @Setter
    private BackPressureStrategy strategy;

    public Connection() {
        this(100, null);
    }
    public Connection(int capacity, BackPressureStrategy strategy) {
        this.buffer = new LinkedBlockingQueue<>(capacity);
        this.strategy = strategy;
    }

    @Getter
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

    public void push(Message message) {
        if (strategy != null) {
            if (!buffer.offer(message)) {
                strategy.handleFull(buffer, message);
            }
        } else {
            try {
                buffer.put(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
