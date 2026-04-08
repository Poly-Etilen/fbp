package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimerNodeTest {
    @Test
    @DisplayName("initialize 후 메시지 생성")
    void test1() throws InterruptedException {
        TimerNode timer = new TimerNode("timer-1", 100);
        Connection connection = new Connection();
        timer.getOutputPort("out").connect(connection);

        timer.initialize();

        Thread.sleep(100);

        Assertions.assertTrue(connection.getBufferSize() > 0);
        timer.shutdown();
    }

    @Test
    @DisplayName("tick 증가")
    void test2() throws InterruptedException {
        TimerNode timer = new TimerNode("timer-1", 100);
        Connection connection = new Connection();
        timer.getOutputPort("out").connect(connection);

        timer.initialize();
        Thread.sleep(250);
        timer.shutdown();

        Message m1 = connection.poll();
        Message m2 = connection.poll();

        Assertions.assertNotNull(m1);
        Assertions.assertNotNull(m2);

        Assertions.assertEquals(0, (int) m1.get("tick"));
        Assertions.assertEquals(1, (int) m2.get("tick"));
    }

    @Test
    @DisplayName("shutdown 후 정지")
    void test3() throws InterruptedException {
        TimerNode timer = new TimerNode("timer-1", 100);
        Connection connection = new Connection();
        timer.getOutputPort("out").connect(connection);

        timer.initialize();
        Thread.sleep(150);
        timer.shutdown();

        int bufferSizeAfterShutdown = connection.getBufferSize();

        Thread.sleep(200);
        Assertions.assertEquals(bufferSizeAfterShutdown, connection.getBufferSize());
    }

    @Test
    @DisplayName("주기 확인")
    void test4() throws InterruptedException {
        TimerNode timer = new TimerNode("timer-1", 500);
        Connection connection = new Connection();
        timer.getOutputPort("out").connect(connection);

        timer.initialize();

        Thread.sleep(2100);
        timer.shutdown();

        int msgCount = connection.getBufferSize();
        Assertions.assertTrue(msgCount >= 4 && msgCount <= 5);
    }
}
