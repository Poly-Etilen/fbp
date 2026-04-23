package com.fbp.engine.node;


import com.fbp.engine.core.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class RuleNodeTest {

    private RuleNode ruleNode;
    private OutputPort matchPort;
    private OutputPort mismatchPort;
    private InputPort inPort;

    private Connection matchConnection;
    private Connection mismatchConnection;

    @BeforeEach
    void setUp() {
        Predicate<Message> tempCondition = msg -> {
            Double temp = (Double) msg.getPayload().get("temperature");
            return temp != null && temp > 30.0;
        };

        ruleNode = new RuleNode("test-rule-node", tempCondition);
        ruleNode.initialize();

        inPort = ruleNode.getInputPort("in");
        matchPort = ruleNode.getOutputPort("match");
        mismatchPort = ruleNode.getOutputPort("mismatch");

        matchConnection = new Connection();
        mismatchConnection = new Connection();
        matchPort.connect(matchConnection);
        mismatchPort.connect(mismatchConnection);
    }

    @Test
    @DisplayName("포트 구성")
    void testPortConfiguration() {
        assertNotNull(inPort);
        assertNotNull(matchPort);
        assertNotNull(mismatchPort);
    }

    @Test
    @DisplayName("조건 만족 → match")
    void testMatchCondition() throws InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 35.0);
        Message message = new Message(payload);

        ruleNode.onProcess(message);

        assertEquals(1, matchConnection.getBufferSize());
        Message outputMsg = matchConnection.poll();
        assertNotNull(outputMsg);

        assertEquals(0, mismatchConnection.getBufferSize());
    }

    @Test
    @DisplayName("조건 불만족 → mismatch")
    void testMismatchCondition() throws InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 25.0);
        Message message = new Message(payload);

        ruleNode.onProcess(message);

        assertEquals(1, mismatchConnection.getBufferSize());
        Message outputMsg = mismatchConnection.poll();
        assertNotNull(outputMsg);
        assertEquals(0, matchConnection.getBufferSize());
    }

    @Test
    @DisplayName("null 필드 처리")
    void testNullFieldHandling() throws InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("humidity", 60.0);
        Message message = new Message(payload);

        assertDoesNotThrow(() -> ruleNode.onProcess(message));

        Message outputMsg = mismatchConnection.poll();
        assertNotNull(outputMsg);
    }

    @Test
    @DisplayName("다수 메시지 분기")
    void testMultipleMessageRouting() throws InterruptedException {
        Message highTempMsg = new Message(Map.of("temperature", 40.0));
        Message lowTempMsg = new Message(Map.of("temperature", 20.0));
        Message normalTempMsg = new Message(Map.of("temperature", 30.0));

        ruleNode.onProcess(highTempMsg);
        ruleNode.onProcess(lowTempMsg);
        ruleNode.onProcess(normalTempMsg);

        assertEquals(highTempMsg, matchConnection.poll());
        assertEquals(lowTempMsg, mismatchConnection.poll());
        assertEquals(normalTempMsg, mismatchConnection.poll());
    }
}