package com.fbp.engine.parser;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.CollectorNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlowManagerTest {
    private NodeRegistry registry;
    private FlowEngine engine;
    private FlowManager flowManager;

    static class DummyNode extends AbstractNode {
        public DummyNode(String id) { super(id); }
        @Override protected void onProcess(Message message) {}
    }

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
        engine = new FlowEngine();
        flowManager = new FlowManager(registry, engine);
        registry.register("SourceType", (id, config) -> new TimerNode(id, 1000));
        registry.register("TargetType", (id, config) -> new CollectorNode(id));
    }

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    private FlowDefinition createSampleDefinition(String flowId) {
        FlowDefinition def = new FlowDefinition();
        def.setId(flowId);
        def.setName("테스트 플로우");

        NodeDefinition n1 = new NodeDefinition();
        n1.setId("node1");
        n1.setType("SourceType");
        n1.setConfig(Map.of());

        NodeDefinition n2 = new NodeDefinition();
        n2.setId("node2");
        n2.setType("TargetType");
        n2.setConfig(Map.of());

        def.setNodes(List.of(n1, n2));

        ConnectionDefinition conn = new ConnectionDefinition();
        conn.setFrom("node1:out");
        conn.setTo("node2:in");
        def.setConnections(List.of(conn));

        return def;
    }

    @Test
    @DisplayName("deploy")
    void testDeploy() {
        FlowDefinition def = createSampleDefinition("flow-1");
        flowManager.deploy(def);

        assertNotNull(flowManager.getFlow("flow-1"));
        assertEquals(Flow.FlowState.RUNNING, flowManager.getFlowStatus("flow-1"));
    }

    @Test
    @DisplayName("list")
    void testList() {
        flowManager.deploy(createSampleDefinition("flow-A"));
        flowManager.deploy(createSampleDefinition("flow-B"));

        Set<String> flowIds = flowManager.getDeployedFlowIds();

        assertEquals(2, flowIds.size());
        assertTrue(flowIds.contains("flow-A"));
        assertTrue(flowIds.contains("flow-B"));
    }

    @Test
    @DisplayName("getStatus")
    void testGetStatus() {
        flowManager.deploy(createSampleDefinition("flow-status"));
        assertEquals(Flow.FlowState.RUNNING, flowManager.getFlowStatus("flow-status"));
        flowManager.stopFlow("flow-status");
        assertEquals(Flow.FlowState.STOPPED, flowManager.getFlowStatus("flow-status"));
    }

    @Test
    @DisplayName("stop")
    void testStop() {
        flowManager.deploy(createSampleDefinition("flow-stop"));
        flowManager.stopFlow("flow-stop");
        assertEquals(Flow.FlowState.STOPPED, flowManager.getFlowStatus("flow-stop"));
    }

    @Test
    @DisplayName("restart")
    void testRestart() {
        flowManager.deploy(createSampleDefinition("flow-restart"));
        flowManager.stopFlow("flow-restart");

        flowManager.restartFlow("flow-restart");
        assertEquals(Flow.FlowState.RUNNING, flowManager.getFlowStatus("flow-restart"));
    }

    @Test
    @DisplayName("remove")
    void testRemove() {
        flowManager.deploy(createSampleDefinition("flow-remove"));
        flowManager.stopFlow("flow-remove");

        flowManager.undeploy("flow-remove");
        assertNull(flowManager.getFlow("flow-remove"));
        assertFalse(flowManager.getDeployedFlowIds().contains("flow-remove"));
    }

    @Test
    @DisplayName("실행 중 삭제")
    void testRemoveWhileRunning() {
        flowManager.deploy(createSampleDefinition("flow-force-remove"));
        assertEquals(Flow.FlowState.RUNNING, flowManager.getFlowStatus("flow-force-remove"));

        flowManager.undeploy("flow-force-remove");
        assertNull(flowManager.getFlow("flow-force-remove"));
        assertNull(flowManager.getFlowStatus("flow-force-remove"));
    }

    @Test
    @DisplayName("존재하지 않는 id 조작")
    void testManipulateNonExistingFlow() {
        String ghostId = "ghost-flow";
        assertThrows(IllegalArgumentException.class, () -> flowManager.stopFlow(ghostId));
        assertThrows(IllegalArgumentException.class, () -> flowManager.restartFlow(ghostId));
        assertThrows(IllegalArgumentException.class, () -> flowManager.undeploy(ghostId));
    }

    @Test
    @DisplayName("중복 id 배포")
    void testDeployDuplicateId() {
        flowManager.deploy(createSampleDefinition("flow-dup"));
        FlowDefinition dupDef = createSampleDefinition("flow-dup");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> flowManager.deploy(dupDef));
        assertTrue(exception.getMessage().contains("이미 동일한 ID"));
    }

    @Test
    @DisplayName("미등록 노드 타입")
    void testUnregisteredNodeType() {
        FlowDefinition def = createSampleDefinition("flow-unknown-type");

        def.getNodes().get(0).setType("UnknownAlienType");
        assertThrows(RuntimeException.class, () -> flowManager.deploy(def));
    }
}
