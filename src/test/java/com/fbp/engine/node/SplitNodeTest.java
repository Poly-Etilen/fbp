package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SplitNodeTest {

    @Test
    @DisplayName("조건 만족 → match 포트")
    void test1() throws InterruptedException {
        SplitNode node = new SplitNode("s1", "val", 10.0);
        Connection connection = new Connection();
        node.getOutputPort("match").connect(connection);

        node.process(new Message(Map.of("val", 15.0)));
        Assertions.assertNotNull(connection.poll());
    }

    @Test
    @DisplayName("조건 미달 → mismatch 포트")
    void test2() throws InterruptedException {
        SplitNode node = new SplitNode("s1", "val", 10.0);
        Connection connection = new Connection();
        node.getOutputPort("mismatch").connect(connection);

        node.process(new Message(Map.of("val", 0.0)));
        Assertions.assertNotNull(connection.poll());
    }

    @Test
    @DisplayName("양쪽 동시 확인")
    void test3() throws InterruptedException {
        SplitNode node = new SplitNode("s1", "val", 10.0);
        Connection matchConnection = new Connection();
        Connection mismatchConnection = new Connection();
        node.getOutputPort("match").connect(matchConnection);
        node.getOutputPort("mismatch").connect(mismatchConnection);

        node.process(new Message(Map.of("val", 15.0)));
        node.process(new Message(Map.of("val", 5.0)));

        Assertions.assertEquals(1, matchConnection.getBufferSize());
        Assertions.assertEquals(1, mismatchConnection.getBufferSize());
    }

    @Test
    @DisplayName("경계값 처리")
    void test4() throws InterruptedException {
        SplitNode node = new SplitNode("s1", "val", 10.0);
        Connection matchConnection = new Connection();
        node.getOutputPort("match").connect(matchConnection);

        node.process(new Message(Map.of("val", 10.0)));
        Assertions.assertNotNull(matchConnection.poll());
    }
}
