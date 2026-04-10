package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ConnectionTest {
    @Test
    @DisplayName("deliver 후 target 수신")
    void test1() throws InterruptedException{
        Connection conn = new Connection();

        Message msg = new Message(Map.of("data", 1));
        conn.deliver(msg);

        Message polledMsg = conn.poll();
        Assertions.assertNotNull(polledMsg);
        Assertions.assertEquals(msg, polledMsg);
    }

    @Test
    @DisplayName("target 미설정 시 동작")
    void test2() {
        Connection conn = new Connection();
        Message msg = new Message(Map.of("data", 1));
        Assertions.assertDoesNotThrow(() -> conn.deliver(msg));
    }

    @Test
    @DisplayName("버퍼 크기 확인")
    void test3() {
        Connection conn = new Connection();
        conn.deliver(new Message(Map.of("data", 1)));
        conn.deliver(new Message(Map.of("data", 2)));

        Assertions.assertEquals(2, conn.getBufferSize());
    }

    @Test
    @DisplayName("다수 메시지 순서 보장")
    void test4() throws InterruptedException {
        Connection conn = new Connection();

        conn.deliver(new Message(Map.of("seq", 1)));
        conn.deliver(new Message(Map.of("seq", 2)));
        conn.deliver(new Message(Map.of("seq", 3)));

        Assertions.assertEquals(1, (int) conn.poll().get("seq"));
        Assertions.assertEquals(2, (int) conn.poll().get("seq"));
        Assertions.assertEquals(3, (int) conn.poll().get("seq"));
    }
}
