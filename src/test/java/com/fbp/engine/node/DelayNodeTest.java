package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DelayNodeTest {
    @Test
    @DisplayName("지연 후 전달")
    void test1() throws InterruptedException {
        DelayNode node = new DelayNode("d1", 500);
        Connection connection = new Connection();
        node.getOutputPort("out").connect(connection);

        long startTime = System.currentTimeMillis();
        node.process(new Message(Map.of("data", "wait")));
        long endTime = System.currentTimeMillis();

        Assertions.assertNotNull(connection.poll());
        Assertions.assertTrue((endTime - startTime) >= 500);
    }

    @Test
    @DisplayName("메시지 내용 보존")
    void test2() throws InterruptedException {
        DelayNode node = new DelayNode("d1", 100);
        Connection connection = new Connection();
        node.getOutputPort("out").connect(connection);

        Message originalMsg = new Message(Map.of("data", "테스트"));
        node.process(originalMsg);

        Message msg = connection.poll();
        Assertions.assertEquals(originalMsg, msg);
    }
}
