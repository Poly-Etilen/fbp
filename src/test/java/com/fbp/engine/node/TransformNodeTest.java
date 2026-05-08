package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class TransformNodeTest {
    @Test
    @DisplayName("변환 정상 동작")
    void test1() throws InterruptedException{
        TransformNode node = new TransformNode("t1", msg -> new Message(Map.of("newKey", "newValue")));
        Connection connection = new LocalConnection();
        node.getOutputPort("out").connect(connection);

        node.process(new Message(Map.of("oldKey", "oldValue")));

        Message result = connection.poll();
        Assertions.assertEquals("newValue", result.get("newKey"));
    }

    @Test
    @DisplayName("null 반환 시 미전달")
    void test2() {
        TransformNode node = new TransformNode("t1", msg -> null);
        Connection connection = new LocalConnection();
        node.getOutputPort("out").connect(connection);

        node.process(new Message(Map.of("key", "value")));
        Assertions.assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("원본 메시지 불변")
    void test3() {
        Message original = new Message(Map.of("key", "originalValue"));
        TransformNode node = new TransformNode("t1", msg -> {
            return new Message(Map.of("key", msg.get("key") + "-modified"));
        });

        node.process(original);
        Assertions.assertEquals("originalValue", original.get("key"));
    }
}
