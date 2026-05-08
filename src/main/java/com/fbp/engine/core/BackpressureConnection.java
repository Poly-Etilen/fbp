package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class BackpressureConnection {
    private final String id = UUID.randomUUID().toString();
    private final BlockingQueue<Message> buffer;
    private final AtomicLong dropCount = new AtomicLong(0);

    @Getter
    @Setter
    private InputPort target;

    @Getter
    @Setter
    private BackPressureStrategy strategy;

    public BackpressureConnection(int capacity, BackPressureStrategy strategy) {
        this.buffer = new LinkedBlockingQueue<>(capacity);
        this.strategy = strategy;
    }

    public void push(Message message) {
        if (!buffer.offer(message)) {
            if (strategy != null) {
                strategy.handleFull(buffer, message);
            } else {
                try {
                    log.debug("[Backpressure] 전략이 없어 공간이 생길 때까지 대기합니다.");
                    buffer.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public Message peek() {
        return buffer.peek();
    }

    public Message poll() throws InterruptedException {
        return buffer.take();
    }

    public int getBufferSize() {
        return buffer.size();
    }

    public long getDropCount() {
        return dropCount.get();
    }
}
