package com.fbp.engine.core.port.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DefaultInputPortTest {
    static class MockNode extends AbstractNode {
        boolean isProcessCalled = false;
        public MockNode(String id) {
            super(id);
        }
        @Override
        protected void onProcess(Message message) {
            isProcessCalled = true;
        }
    }
    @Test
    @DisplayName("receive 시 owner 호출")
    void test1() {
        MockNode mockOwner = new MockNode("mock-node");
        DefaultInputPort in = new DefaultInputPort("in-port", mockOwner);

        in.receive(new Message(Map.of()));
        Assertions.assertTrue(mockOwner.isProcessCalled, "owner 노드의 process()가 호출되어야 함.");
    }

    @Test
    @DisplayName("포트 이름 확인")
    void test2() {
        MockNode mockOwner = new MockNode("mock-node");
        DefaultInputPort in = new DefaultInputPort("test-in", mockOwner);
        Assertions.assertEquals("test-in", in.getName());
    }
}
