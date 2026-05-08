package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TimeWindowRuleNodeTest {

    private Connection alertConnection;
    private Connection passConnection;
    private TimeWindowRuleNode node;

    @BeforeEach
    void setUp() {
        alertConnection = new LocalConnection();
        passConnection = new LocalConnection();

        // 조건: 온도가 30.0 초과
        Predicate<Message> tempOver30 = msg -> {
            Double temp = (Double) msg.getPayload().get("temperature");
            return temp != null && temp > 30.0;
        };

        node = new TimeWindowRuleNode("time-window-node", tempOver30, 100, 3);
        node.initialize();
        node.getOutputPort("alert").connect(alertConnection);
        node.getOutputPort("pass").connect(passConnection);
    }

    @Test
    @DisplayName("기준 미달 → pass")
    void testBelowThreshold() {
        Message msg = new Message(Map.of("temperature", 35.0));

        node.onProcess(msg);
        node.onProcess(msg);

        assertEquals(0, alertConnection.getBufferSize());
        assertEquals(2, passConnection.getBufferSize());
    }

    @Test
    @DisplayName("기준 도달 → alert")
    void testReachThreshold() throws InterruptedException {
        Message msg = new Message(Map.of("temperature", 35.0));

        node.onProcess(msg);
        node.onProcess(msg);
        node.onProcess(msg);

        assertEquals(1, alertConnection.getBufferSize());
        assertEquals(2, passConnection.getBufferSize());
        assertNotNull(alertConnection.poll());
    }

    @Test
    @DisplayName("시간 창 만료")
    void testTimeWindowExpiration() throws InterruptedException {
        Message msg = new Message(Map.of("temperature", 35.0));

        // 처음에 2번 보냄 (이벤트 2개 기록됨)
        node.onProcess(msg);
        node.onProcess(msg);

        Thread.sleep(150);
        node.onProcess(msg);

        assertEquals(0, alertConnection.getBufferSize());
        assertEquals(3, passConnection.getBufferSize());
    }

    @Test
    @DisplayName("조건 불만족 메시지")
    void testConditionNotMet() {
        Message matchMsg = new Message(Map.of("temperature", 35.0));
        Message mismatchMsg = new Message(Map.of("temperature", 25.0));

        // 조건을 만족하는 메시지 2번 전송 (이벤트 2개 기록)
        node.onProcess(matchMsg);
        node.onProcess(matchMsg);

        // 조건을 불만족하는 메시지 5번 전송 (이벤트로 기록되지 않아야 함)
        for (int i = 0; i < 5; i++) {
            node.onProcess(mismatchMsg);
        }

        assertEquals(0, alertConnection.getBufferSize());
        assertEquals(7, passConnection.getBufferSize());
    }
}