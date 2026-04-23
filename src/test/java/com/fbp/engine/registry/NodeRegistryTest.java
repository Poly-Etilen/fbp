package com.fbp.engine.registry;


import com.fbp.engine.core.Node;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NodeRegistryTest {

    private NodeRegistry registry;

    static class MockNode extends AbstractNode {
        private final Map<String, Object> config;

        public MockNode(String id, Map<String, Object> config) {
            super(id);
            this.config = config;
        }

        @Override
        protected void onProcess(Message message) {}
        
        public Map<String, Object> getConfig() { return config; }
    }

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
    }

    @Test
    @DisplayName("register + create")
    void testRegisterAndCreate() {
        registry.register("Mock", (id, config) -> new MockNode(id, config));

        Node node = registry.create("Mock", "test-id", Map.of("key", "value"));

        assertNotNull(node);
        assertEquals("test-id", node.getId());
        assertTrue(node instanceof MockNode);
    }

    @Test
    @DisplayName("미등록 타입 create")
    void testCreateUnregisteredType() {
        assertThrows(NodeRegistryException.class, () -> registry.create("Unknown", "id", Map.of()));
    }

    @Test
    @DisplayName("중복 등록 처리")
    void testDuplicateRegistration() {
        registry.register("Type", (id, config) -> new MockNode(id, config));
        NodeFactory newFactory = (id, config) -> new MockNode(id, config);
        
        assertDoesNotThrow(() -> registry.register("Type", newFactory));
        assertEquals(1, registry.getRegisteredTypes().size());
    }

    @Test
    @DisplayName("getRegisteredTypes")
    void testGetRegisteredTypes() {
        registry.register("TypeA", (id, config) -> new MockNode(id, config));
        registry.register("TypeB", (id, config) -> new MockNode(id, config));

        Set<String> types = registry.getRegisteredTypes();
        assertEquals(2, types.size());
        assertTrue(types.contains("TypeA"));
        assertTrue(types.contains("TypeB"));
    }

    @Test
    @DisplayName("config 전달")
    void testConfigPassing() {
        registry.register("Mock", (id, config) -> new MockNode(id, config));
        Map<String, Object> config = Map.of("threshold", 30.5, "enabled", true);

        MockNode node = (MockNode) registry.create("Mock", "id", config);

        assertEquals(30.5, node.getConfig().get("threshold"));
        assertEquals(true, node.getConfig().get("enabled"));
    }

    @Test
    @DisplayName("isRegistered")
    void testIsRegistered() {
        registry.register("Exists", (id, config) -> new MockNode(id, config));

        assertTrue(registry.isRegistered("Exists"));
        assertFalse(registry.isRegistered("None"));
    }

    @Test
    @DisplayName("null/빈 타입명")
    void testInvalidTypeName() {
        assertThrows(NodeRegistryException.class, () -> registry.register(null, (id, config) -> null));
        assertThrows(NodeRegistryException.class, () -> registry.register("", (id, config) -> null));
    }

    @Test
    @DisplayName("NodeFactory")
    void testFactoryInvalidConfig() {
        // 필수 필드 'topic'이 없으면 예외를 던지는 팩토리 설계
        registry.register("StrictNode", (id, config) -> {
            if (!config.containsKey("topic")) throw new IllegalArgumentException("topic 필수");
            return new MockNode(id, config);
        });

        assertThrows(NodeRegistryException.class, () -> 
            registry.create("StrictNode", "id", Map.of("other", "value")));
    }
}