package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class LogNodeTest {
    @Test
    @DisplayName("메시지 통과 전달")
    void test() throws InterruptedException {
        LogNode logNode = new LogNode("log-1");
        Connection conn = new LocalConnection();

        logNode.getOutputPort("out").connect(conn);

        Message originalMsg = new Message(Map.of("secret", "data"));
        logNode.process(originalMsg);

        Message receivedMsg = conn.poll();
        Assertions.assertNotNull(receivedMsg);
        Assertions.assertEquals(originalMsg, receivedMsg);
    }

    @Test
    @DisplayName("중간 삽입 가능")
    void test2() throws InterruptedException {
        GeneratorNode nodeA = new GeneratorNode("Node-A");
        LogNode logNode = new LogNode("Node-Log");
        PrintNode nodeB = new PrintNode("Node-B");

        Connection connAToLog = new LocalConnection();
        Connection connLogToB = new LocalConnection();

        nodeA.getOutputPort("out").connect(connAToLog);
        logNode.getOutputPort("out").connect(connLogToB);

        nodeA.generate("message", "Hello from A");

        Message msgFromA = connAToLog.poll();
        logNode.process(msgFromA);

        Message finalMsg = connLogToB.poll();
        Assertions.assertNotNull(finalMsg);
        Assertions.assertEquals("Hello from A", finalMsg.get("message"));

        Assertions.assertDoesNotThrow(() -> nodeB.process(finalMsg));
    }
}
