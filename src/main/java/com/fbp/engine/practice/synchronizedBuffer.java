package com.fbp.engine.practice;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class synchronizedBuffer {
    public static void main(String[] args) {
        List<String> buffer = new ArrayList<>();
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    synchronized (buffer) {
                        buffer.add("메시지-" + i);
                        buffer.notifyAll();
                    }
                    Thread.sleep(10);
                }
                synchronized (buffer) {
                    buffer.add("END");
                    buffer.notifyAll();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    synchronized (buffer) {
                        while (buffer.isEmpty()) {
                            buffer.wait();
                        }
                        String data = buffer.remove(0);
                        log.info("소비: {}", data);

                        if ("END".equals(data)) {
                            break;
                        }
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
