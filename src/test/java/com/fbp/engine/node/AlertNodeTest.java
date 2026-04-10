package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class AlertNodeTest {
    @Test
    @DisplayName("정상 처리")
    void test1() {
        AlertNode alert = new AlertNode("alert-1");

        Message msg = new Message(Map.of(
                "sensorId", "t-sensor",
                "temperature", 45.5
        ));

        Assertions.assertDoesNotThrow(() -> alert.process(msg));
    }

    @Test
    @DisplayName("키 누락 처리")
    void test2() {
        AlertNode alert = new AlertNode("alert-1");

        Message emptyMsg = new Message(Map.of("wrong-key", "some-value"));
        Assertions.assertDoesNotThrow(() -> alert.process(emptyMsg));
    }
}
