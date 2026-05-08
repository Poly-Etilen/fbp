package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LocalConnection implements Connection {
    private final String id = UUID.randomUUID().toString();
    private final BlockingQueue<Message> buffer;

    @Getter
    @Setter
    private InputPort inputPort;

    @Getter
    @Setter
    private InputPort target;

    @Setter
    private BackPressureStrategy strategy;

    public LocalConnection() {
        this(100, null);
    }

    public LocalConnection(int capacity, BackPressureStrategy strategy) {
        this.buffer = new LinkedBlockingQueue<>(capacity);
        this.strategy = strategy;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void deliver(Message message) {
        try {
            buffer.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Message poll() throws InterruptedException {
        return buffer.take();
    }

    @Override
    public int getBufferSize() {
        return buffer.size();
    }

    @Override
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

    @Override
    public void close() {
        buffer.clear();
    }
}
