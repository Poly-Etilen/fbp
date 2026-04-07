package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PrintNodeTest {
    @Test
    @DisplayName("InputPort 조회")
    void test1() {
        PrintNode printer = new PrintNode("printer-1");
        Assertions.assertNotNull(printer.getInputPort());
    }

    @Test
    @DisplayName("InputPort를 통한 수신")
    void test2() {
        PrintNode printer = new PrintNode("printer-1");
        Message msg = new Message(Map.of("message", "Hello World!"));

        Assertions.assertDoesNotThrow(() -> printer.getInputPort().receive(msg));
    }
}
