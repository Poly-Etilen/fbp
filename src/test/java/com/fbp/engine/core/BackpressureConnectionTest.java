package com.fbp.engine.core;

import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.core.strategy.impl.DropNewestStrategy;
import com.fbp.engine.core.strategy.impl.DropOldestStrategy;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BackpressureConnectionTest {

    @Test
    @DisplayName("Block 전략")
    void testBlockStrategy() throws InterruptedException {
        BackpressureConnection conn = new BackpressureConnection(2, null);
        conn.push(new Message(Map.of("id", 1)));
        conn.push(new Message(Map.of("id", 2)));

        Thread producer = new Thread(() -> conn.push(new Message(Map.of("id", 3))));
        producer.start();

        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, producer.getState());

        conn.poll();
        Thread.sleep(100);
        assertEquals(Thread.State.TERMINATED, producer.getState());
    }

    @Test
    @DisplayName("DropOldest 전략")
    void testDropOldestStrategy() throws InterruptedException {
        BackpressureConnection conn = new BackpressureConnection(2, new DropOldestStrategy());
        conn.push(new Message(Map.of("id", 1)));
        conn.push(new Message(Map.of("id", 2)));
        conn.push(new Message(Map.of("id", 3)));

        assertEquals(2, conn.poll().getPayload().get("id"));
        assertEquals(3, conn.poll().getPayload().get("id"));
    }

    @Test
    @DisplayName("DropNewest 전략")
    void testDropNewestStrategy() throws InterruptedException {
        BackpressureConnection conn = new BackpressureConnection(2, new DropNewestStrategy());
        conn.push(new Message(Map.of("id", 1)));
        conn.push(new Message(Map.of("id", 2)));
        conn.push(new Message(Map.of("id", 3)));

        assertEquals(1, conn.poll().getPayload().get("id"));
        assertEquals(2, conn.poll().getPayload().get("id"));
        assertEquals(0, conn.getBufferSize());
    }

    @Test
    @DisplayName("전략 변경")
    void testRuntimeStrategyChange() throws InterruptedException {
        BackpressureConnection conn = new BackpressureConnection(1, new DropNewestStrategy());
        conn.push(new Message(Map.of("id", "original")));

        conn.push(new Message(Map.of("id", "newest")));
        assertEquals("original", conn.peek().getPayload().get("id"), "DropNewest에 의해 original이 남아야 함");

        conn.setStrategy(new DropOldestStrategy());
        conn.push(new Message(Map.of("id", "replacement")));

        assertEquals("replacement", conn.poll().getPayload().get("id"), "전략 변경 후 original이 드롭되고 replacement가 들어가야 함");
    }

    @Test
    @DisplayName("큐 크기 설정")
    void testCapacityLimit() {
        int capacity = 5;
        BackpressureConnection conn = new BackpressureConnection(capacity, new DropNewestStrategy());

        for (int i = 0; i < 10; i++) {
            conn.push(new Message(Map.of("id", i)));
        }

        assertEquals(capacity, conn.getBufferSize());
    }

    @Test
    @DisplayName("드롭 카운트")
    void testDropCountMetric() {
        BackPressureStrategy mockStrategy = mock(BackPressureStrategy.class);
        BackpressureConnection conn = new BackpressureConnection(1, mockStrategy);

        conn.push(new Message(Map.of("id", 0)));

        for (int i = 0; i < 5; i++) {
            conn.push(new Message(Map.of("id", i + 1)));
        }

        verify(mockStrategy, times(5)).handleFull(any(), any());
    }

    @Test
    @DisplayName("멀티스레드")
    void testMultithreadSafety() throws InterruptedException {
        int capacity = 100;
        int threadCount = 10;
        int messagesPerThread = 100;
        int totalAttempted = threadCount * messagesPerThread;

        BackPressureStrategy mockStrategy = mock(BackPressureStrategy.class);
        BackpressureConnection conn = new BackpressureConnection(capacity, mockStrategy);

        Thread[] producers = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            producers[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    conn.push(new Message(Map.of("data", "test")));
                }
            });
            producers[i].start();
        }

        for (Thread t : producers) t.join();

        assertEquals(capacity, conn.getBufferSize());
        int expectedDrops = totalAttempted - capacity;
        verify(mockStrategy, times(expectedDrops)).handleFull(any(), any());
    }
}
