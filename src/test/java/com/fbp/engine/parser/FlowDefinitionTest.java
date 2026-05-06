package com.fbp.engine.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlowDefinitionTest {

    private FlowDefinition flowDef;

    @BeforeEach
    void setUp() {
        flowDef = new FlowDefinition();

        NodeDefinition node1 = new NodeDefinition();
        node1.setId("sensor");
        NodeDefinition node2 = new NodeDefinition();
        node2.setId("rule");

        List<NodeDefinition> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        flowDef.setNodes(nodes);

        ConnectionDefinition conn = new ConnectionDefinition();
        conn.setFrom("sensor:out");
        conn.setTo("rule:in");
        
        List<ConnectionDefinition> connections = new ArrayList<>();
        connections.add(conn);
        flowDef.setConnections(connections);
    }

    @Test
    @DisplayName("불변성")
    void test1_Immutability() {
        assertThrows(UnsupportedOperationException.class, () -> flowDef.getNodes().add(new NodeDefinition()));
        assertThrows(UnsupportedOperationException.class, () -> flowDef.getConnections().add(new ConnectionDefinition()));
    }

    @Test
    @DisplayName("노드 조회")
    void test2_GetNode() {
        NodeDefinition sensorNode = flowDef.getNode("sensor");
        assertNotNull(sensorNode);
        assertEquals("sensor", sensorNode.getId());
        assertNull(flowDef.getNode("unknown"));
    }

    @Test
    @DisplayName("연결 유효성")
    void test3_ConnectionValidity() {
        assertDoesNotThrow(() -> flowDef.validate());

        ConnectionDefinition badConn = new ConnectionDefinition();
        badConn.setFrom("sensor:out");
        badConn.setTo("ghost-node:in");

        List<ConnectionDefinition> badConnections = new ArrayList<>(flowDef.getConnections());
        badConnections.add(badConn);
        flowDef.setConnections(badConnections);

        assertThrows(IllegalStateException.class, () -> flowDef.validate());
    }
}