package com.fbp.engine.registry;

import com.fbp.engine.core.Node;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeFactoryTest {

    private NodeRegistry registry;

    // 테스트용 목(Mock) 노드
    static class TestNode extends AbstractNode {
        public TestNode(String id) { super(id); }
        @Override protected void onProcess(Message message) {}
    }

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
    }

    @Test
    @DisplayName("정상 생성")
    void testNormalCreation() {
        registry.register("TestType", new NodeFactory() {
            @Override
            public Node create(String id, Map<String, Object> config) {
                return new TestNode(id);
            }
        });

        Node node = registry.create("TestType", "node-1", Map.of());

        assertNotNull(node);
        assertEquals("node-1", node.getId());
        assertTrue(node instanceof TestNode);
    }

    @Test
    @DisplayName("잘못된 config")
    void testInvalidConfig() {

        registry.register("MqttNode", (id, config) -> {
            if (!config.containsKey("topic")) {
                throw new IllegalArgumentException("필수 설정 'topic'이 누락되었습니다.");
            }
            return new TestNode(id);
        });


        NodeRegistryException exception = assertThrows(NodeRegistryException.class, () -> {
            registry.create("MqttNode", "node-error", Map.of("other", "value"));
        });
        
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("람다 구현")
    void testLambdaImplementation() {
        assertDoesNotThrow(() -> {
            registry.register("LambdaNode", (id, config) -> new TestNode(id));
        });

        Node node = registry.create("LambdaNode", "lambda-id", Map.of());
        assertNotNull(node);
        assertEquals("lambda-id", node.getId());
    }
}