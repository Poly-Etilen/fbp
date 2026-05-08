package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErrorPortTest {
    private TestNode node;
    private Connection mockErrorConn;

    static class TestNode extends AbstractNode {
        boolean shouldThrow = false;
        public TestNode(String id) { super(id); }
        @Override
        protected void onProcess(Message message) {
            if (shouldThrow) throw new RuntimeException("Test Exception");
        }
    }

    @BeforeEach
    void setUp() {
        node = new TestNode("test-node");
        mockErrorConn = mock(Connection.class);
        node.getErrorPort().connect(mockErrorConn);
    }

    @Test
    @DisplayName("에러 발생 시 분기")
    void testErrorBranching() {
        node.shouldThrow = true;
        Message msg = new Message(Map.of("data", "test"));
        node.process(msg);
        verify(mockErrorConn, times(1)).push(any(Message.class));
    }

    @Test
    @DisplayName("에러 메시지 내용")
    void testErrorMessageContent() {
        node.shouldThrow = true;
        Message originalMsg = new Message(Map.of("data", "payload"));
        final Message[] captured = new Message[1];

        doAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            return null;
        }).when(mockErrorConn).push(any(Message.class));

        node.process(originalMsg);

        Message result = captured[0];
        assertNotNull(result);
        assertEquals("payload", result.getPayload().get("data"));
        assertEquals("test-node", result.getPayload().get("error_node"));
        assertEquals("Test Exception", result.getPayload().get("error_msg"));
        assertEquals("RuntimeException", result.getPayload().get("error_type"));
    }

    @Test
    @DisplayName("에러 포트 미연결")
    void testUnconnectedErrorPort() {
        node.getErrorPort().disconnect(mockErrorConn);
        node.shouldThrow = true;

        assertDoesNotThrow(() -> node.process(new Message(Map.of())));
    }

    @Test
    @DisplayName("정상 처리 시")
    void testNoErrorOnSuccess() {
        node.shouldThrow = false;
        node.process(new Message(Map.of()));
        verify(mockErrorConn, never()).push(any());
    }
}
