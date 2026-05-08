package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class ErrorHandlerNodeTest {
    private ErrorHandlerNode errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = spy(new ErrorHandlerNode("handler-1", 2));
    }

    @Test
    @DisplayName("ErrorHandlerNode 수신")
    void testErrorHandlerReception() {
        Message incomingErrorMsg = new Message(Map.of(
                "error_node", "source-id",
                "retry_count", 0
        ));

        errorHandler.onProcess(incomingErrorMsg);
        verify(errorHandler, atLeastOnce()).send(anyString(), any(Message.class));
    }
    @Test
    @DisplayName("재시도 로직")
    void testRetryLogic() {
        Message errorMsg = new Message(Map.of(
                "error_node", "worker-1",
                "retry_count", 0
        ));

        errorHandler.onProcess(errorMsg);
        verify(errorHandler, times(1)).send(eq("retry"), any(Message.class));
    }

    @Test
    @DisplayName("DeadLetterNode")
    void testDeadLetterLogic() {
        Message errorMsg = new Message(Map.of(
                "error_node", "worker-1",
                "retry_count", 2
        ));

        errorHandler.onProcess(errorMsg);

        verify(errorHandler, times(1)).send(eq("failed"), any(Message.class));
        verify(errorHandler, never()).send(eq("retry"), any());
    }
}
