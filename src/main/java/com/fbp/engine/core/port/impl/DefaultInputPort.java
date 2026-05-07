package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class DefaultInputPort implements InputPort {
    @Getter
    private final String name;
    private final Node owner;
    private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

    @Override
    public void receive(Message message) {
        queue.offer(message);
        try {
            owner.process(message);
        } finally {
            queue.poll();
        }
    }

    @Override
    public int getQueueSize() {
        return queue.size();
    }
}
