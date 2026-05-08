package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.CompositeRuleNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class CompositeRuleNodeTest {

    private Connection matchConnection;
    private Connection mismatchConnection;
    private final Predicate<Message> tempOver30 = msg -> {
        Double temp = (Double) msg.getPayload().get("temperature");
        return temp != null && temp > 30.0;
    };

    private final Predicate<Message> humOver50 = msg -> {
        Double hum = (Double) msg.getPayload().get("humidity");
        return hum != null && hum > 50.0;
    };

    @BeforeEach
    void setUp() {
        matchConnection = new LocalConnection();
        mismatchConnection = new LocalConnection();
    }

    /**
     * 노드에 Connection을 연결하고 테스트하기 위한 헬퍼 메서드
     */
    private void setupNodeConnections(CompositeRuleNode node) {
        node.initialize();
        node.getOutputPort("match").connect(matchConnection);
        node.getOutputPort("mismatch").connect(mismatchConnection);
    }

    @Test
    @DisplayName("AND — 모두 만족")
    void testAndAllConditionsMet() throws InterruptedException {
        CompositeRuleNode node = new CompositeRuleNode("and-node", CompositeRuleNode.Operator.AND);
        node.addCondition(tempOver30);
        node.addCondition(humOver50);
        setupNodeConnections(node);

        Message msg = new Message(Map.of("temperature", 35.0, "humidity", 60.0));
        node.onProcess(msg);

        assertEquals(1, matchConnection.getBufferSize());
        assertEquals(0, mismatchConnection.getBufferSize());
        assertNotNull(matchConnection.poll());
    }

    @Test
    @DisplayName("AND — 하나 불만족")
    void testAndOneConditionUnmet() throws InterruptedException {
        CompositeRuleNode node = new CompositeRuleNode("and-node", CompositeRuleNode.Operator.AND);
        node.addCondition(tempOver30);
        node.addCondition(humOver50);
        setupNodeConnections(node);

        Message msg = new Message(Map.of("temperature", 35.0, "humidity", 40.0));
        node.onProcess(msg);

        assertEquals(1, mismatchConnection.getBufferSize());
        assertEquals(0, matchConnection.getBufferSize());
        assertNotNull(mismatchConnection.poll());
    }

    @Test
    @DisplayName("OR — 하나 만족")
    void testOrOneConditionMet() throws InterruptedException {
        CompositeRuleNode node = new CompositeRuleNode("or-node", CompositeRuleNode.Operator.OR);
        node.addCondition(tempOver30);
        node.addCondition(humOver50);
        setupNodeConnections(node);

        Message msg = new Message(Map.of("temperature", 25.0, "humidity", 60.0));
        node.onProcess(msg);

        assertEquals(1, matchConnection.getBufferSize());
        assertEquals(0, mismatchConnection.getBufferSize());
        assertNotNull(matchConnection.poll());
    }

    @Test
    @DisplayName("OR — 모두 불만족")
    void testOrAllConditionsUnmet() throws InterruptedException {
        CompositeRuleNode node = new CompositeRuleNode("or-node", CompositeRuleNode.Operator.OR);
        node.addCondition(tempOver30);
        node.addCondition(humOver50);
        setupNodeConnections(node);

        Message msg = new Message(Map.of("temperature", 25.0, "humidity", 40.0));
        node.onProcess(msg);

        assertEquals(1, mismatchConnection.getBufferSize());
        assertEquals(0, matchConnection.getBufferSize());
        assertNotNull(mismatchConnection.poll());
    }

    @Test
    @DisplayName("빈 조건")
    void testEmptyConditions() throws InterruptedException {
        CompositeRuleNode andNode = new CompositeRuleNode("empty-and-node", CompositeRuleNode.Operator.AND);
        setupNodeConnections(andNode);
        
        Message msg1 = new Message(Map.of("data", "test"));
        andNode.onProcess(msg1);
        
        assertEquals(1, matchConnection.getBufferSize());
        assertEquals(0, mismatchConnection.getBufferSize());
        matchConnection.poll(); // 큐 비우기

        // OR 노드 (조건 없음)
        CompositeRuleNode orNode = new CompositeRuleNode("empty-or-node", CompositeRuleNode.Operator.OR);
        orNode.initialize();
        orNode.getOutputPort("match").connect(matchConnection);
        orNode.getOutputPort("mismatch").connect(mismatchConnection);

        Message msg2 = new Message(Map.of("data", "test"));
        orNode.onProcess(msg2);

        assertEquals(1, mismatchConnection.getBufferSize());
        assertEquals(0, matchConnection.getBufferSize());
        mismatchConnection.poll();
    }
}