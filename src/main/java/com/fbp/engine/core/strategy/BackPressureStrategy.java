package com.fbp.engine.core.strategy;

import com.fbp.engine.message.Message;

import java.util.concurrent.BlockingQueue;

public interface BackPressureStrategy {
    void handleFull(BlockingQueue<Message> queue, Message newMessage);
}
