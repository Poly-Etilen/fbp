package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DefaultOutputPortTest {
    @Test
    @DisplayName("단일 Connection 전달")
    void test1() {
        DefaultOutputPort out = new DefaultOutputPort("out");
        Connection conn1 = new Connection();
        out.connect(conn1);

        out.send(new Message(Map.of("hello", "world")));
        Assertions.assertEquals(1, conn1.getBufferSize());
    }

    @Test
    @DisplayName("다중 Connection 전달 (1:N)")
    void test2() {
        DefaultOutputPort out = new DefaultOutputPort("out");
        Connection conn1 = new Connection();
        Connection conn2 = new Connection();
        out.connect(conn1);
        out.connect(conn2);

        out.send(new Message(Map.of("hello", "world")));
        Assertions.assertEquals(1, conn1.getBufferSize());
        Assertions.assertEquals(1, conn2.getBufferSize());
    }

    @Test
    @DisplayName("Connection 미연결 시")
    void test3() {
        DefaultOutputPort out = new DefaultOutputPort("out");
        Assertions.assertDoesNotThrow((() -> out.send(new Message(Map.of()))));
    }
}
