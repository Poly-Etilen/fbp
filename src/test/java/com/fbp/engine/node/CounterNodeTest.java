package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CounterNodeTest {

    @Test
    @DisplayName("count 키 추가")
    void test1() throws InterruptedException{
        CounterNode node = new CounterNode("c1");
        Connection connection = new LocalConnection();
        node.getOutputPort("out").connect(connection);

        node.process(new Message(Map.of("data", "A")));
        Message msg = connection.poll();

        Assertions.assertEquals(1, (int) msg.get("count"));
    }

    @Test
    @DisplayName("count 누적")
    void test2() throws InterruptedException{
        CounterNode node = new CounterNode("c1");
        Connection connection = new LocalConnection();
        node.getOutputPort("out").connect(connection);

        node.process(new Message(Map.of("data", "A")));
        node.process(new Message(Map.of("data", "B")));
        node.process(new Message(Map.of("data", "C")));

        connection.poll();
        connection.poll(); // 버리기
        Message msg = connection.poll();

        Assertions.assertEquals(3, (int) msg.get("count"));
    }

    @Test
    @DisplayName("원본 키 유지")
    void test3() throws InterruptedException{
        CounterNode node = new CounterNode("c1");
        Connection connection = new LocalConnection();
        node.getOutputPort("out").connect(connection);

        node.process(new Message(Map.of("originalKey", "originalValue")));
        Message msg = connection.poll();

        Assertions.assertEquals(1, (int) msg.get("count"));
    }

}
