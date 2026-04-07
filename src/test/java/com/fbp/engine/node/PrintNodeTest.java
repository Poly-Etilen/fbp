package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PrintNodeTest {
    @Test
    @DisplayName("getId 반환")
    void test1() {
        PrintNode printer = new PrintNode("printer-1");
        Assertions.assertEquals("printer-1", printer.getId());
    }

    @Test
    @DisplayName("process 정상 동작")
    void test2() {
        PrintNode printer = new PrintNode("printer-1");
        Message msg = new Message(Map.of("temperature", 25.5));

        Assertions.assertDoesNotThrow(() -> printer.process(msg));
    }

    @Test
    @DisplayName("Node 인터페이스 구현")
    void test3() {
        PrintNode printer = new PrintNode("printer-1");
        Assertions.assertTrue(printer instanceof Node);
    }
}
