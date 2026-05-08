package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class BlockingQueueConnectionTest {
    @Test
    @DisplayName("deliver-poll 기본 동작")
    void test1() throws InterruptedException {
        Connection conn = new Connection();
        Message msg = new Message(Map.of("id", "A"));

        conn.deliver(msg);
        Assertions.assertEquals(msg, conn.poll());
    }

    @Test
    @DisplayName("메시지 순서 보장")
    void test2() throws InterruptedException {
        Connection conn = new Connection();
        conn.deliver(new Message(Map.of("seq", 1)));
        conn.deliver(new Message(Map.of("seq", 2)));
        conn.deliver(new Message(Map.of("seq", 3)));

        Assertions.assertEquals(1, (int) conn.poll().get("seq"));
        Assertions.assertEquals(2, (int) conn.poll().get("seq"));
        Assertions.assertEquals(3, (int) conn.poll().get("seq"));
    }

    @Test
    @DisplayName("멀티스레드 deliver-poll")
    void test3() throws InterruptedException {
        Connection conn = new Connection();
        CountDownLatch latch = new CountDownLatch(1);
        Message msg = new Message(Map.of("data", "test"));

        new Thread(() -> {
            try {
                Thread.sleep(100);
                conn.deliver(msg);
                latch.countDown();
            } catch (InterruptedException e) {}
        }).start();

        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
        Assertions.assertEquals("test", conn.poll().get("data"));
    }

    @Test
    @DisplayName("poll 대기 동작")
    void test4() {
        Connection conn = new Connection();
        Assertions.assertThrows(AssertionFailedError.class, () -> {
            Assertions.assertTimeoutPreemptively(Duration.ofMillis(100), () -> conn.poll());
        });
    }

    @Test
    @DisplayName("버퍼 크기 제한")
    void test5() throws InterruptedException {
        Connection conn = new Connection(2, null);
        conn.deliver(new Message(Map.of("id", 1)));
        conn.deliver(new Message(Map.of("id", 2)));

        Thread producer = new Thread(() -> {
            conn.deliver(new Message(Map.of("id", 3)));
        });
        producer.start();

        Thread.sleep(100);
        Assertions.assertEquals(Thread.State.WAITING, producer.getState());

        conn.poll();
        Thread.sleep(100);
        Assertions.assertEquals(Thread.State.TERMINATED, producer.getState());
    }
}
