package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import lombok.Setter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection {
    // Connection은 메세지를 잠깐 저장하는 버퍼 역할을 함.
    private final BlockingQueue<Message> buffer =  new LinkedBlockingQueue<>(100);
    @Setter
    private InputPort target;

    public void transmit(Message message) {
        try {
            buffer.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Message receive() {
        try {
            return buffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
