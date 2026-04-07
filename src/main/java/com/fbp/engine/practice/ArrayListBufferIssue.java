package com.fbp.engine.practice;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ArrayListBufferIssue {
    public static void main(String[] args) {
        List<String> buffer = new ArrayList<>();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                buffer.add("메시지-" + i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
            buffer.add("END");
        });

        Thread consumer = new Thread(() -> {
            while (true) {
                if (!buffer.isEmpty()) {
                    String data = buffer.remove(0);
                    log.info("Consuming data: " + data);
                    if ("END".equals(data)) {
                        break;
                    }
                }
            }
        });
        producer.start();
        consumer.start();
    }
}
