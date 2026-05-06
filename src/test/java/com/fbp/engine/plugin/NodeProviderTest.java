package com.fbp.engine.plugin;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeProviderTest {

    // 테스트용 노드
    static class DummyNode extends AbstractNode {
        public DummyNode(String id) { super(id); }
        @Override protected void onProcess(Message message) {}
    }

    @Test
    @DisplayName("getNodeDescriptors")
    void test1_GetNodeDescriptors() {
        NodeProvider provider = () -> List.of(
                new NodeDescriptor("TestType", "Test desc", DummyNode.class, (id, conf) -> new DummyNode(id))
        );

        List<NodeDescriptor> descriptors = provider.getNodeDescriptors();
        assertEquals(1, descriptors.size());
        assertEquals("TestType", descriptors.getFirst().typeName());
    }

    @Test
    @DisplayName("빈 목록")
    void test2_EmptyList() {
        NodeProvider emptyProvider = Collections::emptyList;
        assertTrue(emptyProvider.getNodeDescriptors().isEmpty());
    }

    @Test
    @DisplayName("descriptor 정합성")
    void test3_DescriptorIntegrity() {
        NodeProvider provider = () -> List.of(
                new NodeDescriptor(null, "No type", DummyNode.class, null)
        );

        NodeDescriptor desc = provider.getNodeDescriptors().getFirst();
        assertNull(desc.typeName());
        assertNull(desc.factory());
    }
}