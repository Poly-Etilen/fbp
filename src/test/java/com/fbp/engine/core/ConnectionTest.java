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
    void test1() {
        Connection conn = new Connection();
        List<Message> received = new ArrayList<>();

        InputPort mockTarget = new InputPort() {
            @Override
            public String getName() {return "mockTarget";}
            @Override
            public void receive(Message message) {
                received.add(message);
            }
        };
        conn.setTarget(mockTarget);

        Message msg = new Message(Map.of("data", 1));
        conn.deliver(msg);

        Assertions.assertEquals(1, received.size(), "타겟이 메시지를 수신해야 함.");
        Assertions.assertEquals(msg, received.getFirst());
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
    void test4() {
        Connection conn = new Connection();
        List<Message> received = new ArrayList<>();
        conn.setTarget(new InputPort() {
            @Override
            public String getName() {return "in";}
            @Override
            public void receive(Message message) {
                received.add(message);
            }
        });
        conn.deliver(new Message(Map.of("seq", 1)));
        conn.deliver(new Message(Map.of("seq", 2)));
        conn.deliver(new Message(Map.of("seq", 3)));

        Assertions.assertEquals(1, (int) received.getFirst().get("seq"));
        Assertions.assertEquals(2, (int) received.get(1).get("seq"));
        Assertions.assertEquals(3, (int) received.get(2).get("seq"));
    }
}
