package com.fbp.engine.practice;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class BlockingQueueBuffer {
    public static void main(String[] args) {
        BlockingQueue<String> buffer = new LinkedBlockingQueue<>(10);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    buffer.put("메시지-" + i);
                    Thread.sleep(10);
                }
                buffer.put("END");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String data = buffer.take();
                    log.info("소비: {}", data);
                    if ("END".equals(data)) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        consumer.start();
    }
}
