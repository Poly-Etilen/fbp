package com.fbp.engine.core.strategy.impl;

import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class DropNewestStrategy implements BackPressureStrategy {
    @Override
    public void handleFull(BlockingQueue<Message> queue, Message newMessage) {
        log.warn("[Backpressure] 큐가 가득 차 새로운 메시지를 버립니다.");
    }
}
