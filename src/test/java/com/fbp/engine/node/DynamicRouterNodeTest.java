package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicRouterNodeTest {
    private DynamicRouterNode router;

    @BeforeEach
    void setUp() {
        router = spy(new DynamicRouterNode("router-1"));
    }

    @Test
    @DisplayName("조건 매칭")
    void testConditionMatching() {
        router.addRule("temp-port", msg -> msg.getPayload().containsKey("temp"));
        Message tempMsg = new Message(Map.of("temp", 25.5));

        router.onProcess(tempMsg);
        verify(router, times(1)).send("temp-port", tempMsg);
    }

    @Test
    @DisplayName("다중 규칙")
    void testMultipleRules() {
        router.addRule("first-port", msg -> true);
        router.addRule("second-port", msg -> true);
        Message msg = new Message(Map.of("any", "data"));

        router.onProcess(msg);

        verify(router, times(1)).send("first-port", msg);
        verify(router, never()).send("second-port", msg);
    }

    @Test
    @DisplayName("기본 포트")
    void testDefaultPortWhenNoMatch() {
        router.addRule("sensor-port", msg -> "sensor".equals(msg.getPayload().get("type")));

        Message unmatchedMsg = new Message(Map.of("type", "actuator"));
        router.onProcess(unmatchedMsg);

        verify(router, times(1)).send("default", unmatchedMsg);
        verify(router, never()).send("sensor-port", unmatchedMsg);
    }

    @Test
    @DisplayName("규칙 없음")
    void testDefaultPortWhenRulesEmpty() {
        Message anyMsg = new Message(Map.of("data", "anything"));

        router.onProcess(anyMsg);

        verify(router, times(1)).send("default", anyMsg);
    }

    @Test
    @DisplayName("null 필드")
    void testNullFieldHandling() {
        router.addRule("type-port", msg -> "sensor".equals(msg.getPayload().get("type")));
        Message msgWithoutType = new Message(Map.of("other", "value"));

        assertDoesNotThrow(() -> router.onProcess(msgWithoutType));
        verify(router, times(1)).send("default", msgWithoutType);
    }

    @Test
    @DisplayName("성능")
    void testPerformanceWithManyRules() {
        for (int i = 0; i < 99; i++) {
            router.addRule("port-" + i, msg -> false);
        }
        router.addRule("last-port", msg -> true);
        Message msg = new Message(Map.of("test", "data"));

        long start = System.currentTimeMillis();
        router.onProcess(msg);
        long end = System.currentTimeMillis();

        assertTrue((end - start) < 10, "100개 규칙 처리 시간이 너무 오래 걸립니다.");
        verify(router, times(1)).send("last-port", msg);
    }
}