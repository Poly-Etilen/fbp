package com.fbp.engine.core.strategy.impl;

import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class DropOldestStrategy implements BackPressureStrategy {
    @Override
    public void handleFull(BlockingQueue<Message> queue, Message newMessage) {
        Message dropped = queue.poll();
        if (dropped != null) {
            log.warn("[Backpressure] 큐가 가득 차 가장 오래된 메시지를 드롭합니다.");
        }
        queue.offer(newMessage);
    }
}
