package com.fbp.engine.flow;

import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.parser.NodeDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubFlowNodeTest {
    private Flow mockInternalFlow;
    private SubFlowNode subFlowNode;
    private AbstractNode mockInternalNode;

    @BeforeEach
    void setUp() {
        mockInternalFlow = mock(Flow.class);
        mockInternalNode = mock(AbstractNode.class);
        subFlowNode = new SubFlowNode("sub-flow-1", mockInternalFlow);
        when(mockInternalFlow.getNode("sensor-logic")).thenReturn(mockInternalNode);
    }

    @Test
    @DisplayName("메시지 전달")
    void testExternalToInternalDelivery() {
        subFlowNode.mapInput("in", "sensor-logic");
        Message incomingMsg = new Message(Map.of("value", 100));

        subFlowNode.process(incomingMsg);

        verify(mockInternalNode, times(1)).process(incomingMsg);
    }

    @Test
    @DisplayName("내부 플로우 실행")
    void testInternalFlowSequence() {
        AbstractNode mockNodeA = mock(AbstractNode.class);
        AbstractNode mockNodeB = mock(AbstractNode.class);

        when(mockInternalFlow.getNode("nodeA")).thenReturn(mockNodeA);
        subFlowNode.mapInput("in", "nodeA");

        Message startMsg = new Message(Map.of("step", "start"));
        subFlowNode.process(startMsg);
        verify(mockNodeA, times(1)).process(startMsg);
    }

    @Test
    @DisplayName("수명주기 - 시작")
    void testLifecycleStart() {
        subFlowNode.initialize();
        verify(mockInternalFlow, times(1)).initialize();
    }

    @Test
    @DisplayName("수명주기 - 정지")
    void testLifecycleStop() {
        subFlowNode.shutdown();
        verify(mockInternalFlow, times(1)).shutdown();
    }

    @Test
    @DisplayName("재사용")
    void testSubFlowReusability() {
        Flow internalFlow1 = new Flow("internal-1", "Logic A");
        Flow internalFlow2 = new Flow("internal-2", "Logic A");

        AbstractNode nodeA1 = mock(AbstractNode.class);
        AbstractNode nodeA2 = mock(AbstractNode.class);

        when(nodeA1.getId()).thenReturn("node-A");
        when(nodeA2.getId()).thenReturn("node-A");

        internalFlow1.addNode(nodeA1);
        internalFlow2.addNode(nodeA2);

        SubFlowNode subNode1 = new SubFlowNode("instance-1", internalFlow1);
        SubFlowNode subNode2 = new SubFlowNode("instance-2", internalFlow2);

        subNode1.mapInput("in", nodeA1.getId());
        subNode2.mapInput("in", nodeA2.getId());

        Message msg = new Message(Map.of("test", "data"));
        subNode1.process(msg);

        verify(nodeA1, times(1)).process(msg);
        verify(nodeA2, never()).process(msg);
    }

    @Test
    @DisplayName("내부 에러 전파")
    void testErrorPropagation() {
        subFlowNode.mapInput("in", "internal-node");
        doThrow(new RuntimeException("Internal Error")).when(mockInternalNode).process(any());

        Message message = new Message(Map.of("data", "bad-data"));
        assertDoesNotThrow(() -> subFlowNode.process(message));
    }

    @Test
    @DisplayName("JSON 정의")
    void testSubFlowJsonParsing() {
        String json = """
    {
      "id": "parent-flow",
      "nodes": [
        {
          "id": "my-subflow",
          "type": "SubFlow",
          "config": {
            "internalFlowId": "sub-1",
            "inputs": { "in": "internal-node-id" }
          }
        }
      ],
      "connections": []
    }
    """;

        JsonFlowParser parser = new JsonFlowParser();
        FlowDefinition definition = parser.parse(new ByteArrayInputStream(json.getBytes()));

        assertNotNull(definition);
        assertEquals("parent-flow", definition.getId());

        NodeDefinition subNodeDef = definition.getNodes().getFirst();
        assertEquals("my-subflow", subNodeDef.getId());
        assertEquals("SubFlow", subNodeDef.getType());

        Map<String, Object> config = subNodeDef.getConfig();
        assertEquals("sub-1", config.get("internalFlowId"));
    }
}