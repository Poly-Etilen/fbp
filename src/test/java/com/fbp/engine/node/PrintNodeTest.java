package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class PrintNodeTest {
    @Test
    @DisplayName("포트 구성 확인")
    void test1() {
        PrintNode printer = new PrintNode("printer-1");
        Assertions.assertNotNull(printer.getInputPort("in"));
        Assertions.assertNull(printer.getOutputPort("out"));
    }

    @Test
    @DisplayName("process 정상 동작")
    void test2() {
        PrintNode printer = new PrintNode("printer-1");
        Message msg = new Message(Map.of("data", "Hello World!"));

        Assertions.assertDoesNotThrow(() -> printer.process(msg));
    }

    @Test
    @DisplayName("AbstractNode 상속 확인")
    void test3() {
        PrintNode printer = new PrintNode("printer-1");
        Assertions.assertInstanceOf(AbstractNode.class, printer);
        Assertions.assertTrue(printer instanceof AbstractNode);
    }
}
