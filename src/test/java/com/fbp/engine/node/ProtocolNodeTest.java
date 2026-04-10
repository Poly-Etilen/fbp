package com.fbp.engine.node;

import com.fbp.engine.core.ConnectionState;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ProtocolNodeTest {
    @Test
    @DisplayName("초기 상태")
    void test1() {
        EchoProtocolNode node = new EchoProtocolNode("echo-node", Map.of());
        Assertions.assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
        Assertions.assertFalse(node.isConnected());
    }

    @Test
    @DisplayName("config 조회")
    void test2() {
        Map<String, Object> config = Map.of("host", "127.0.0.1", "port", 8888);
        EchoProtocolNode node = new EchoProtocolNode("echo-node", config);

        Assertions.assertEquals("127.0.0.1", node.getConfig("host"));
        Assertions.assertEquals(8888, node.getConfig("port"));
    }

    @Test
    @DisplayName("initialize → CONNECTED")
    void test3() {
        EchoProtocolNode node = new EchoProtocolNode("echo-node", Map.of());
        node.initialize();
        Assertions.assertEquals(ConnectionState.CONNECTED, node.getConnectionState());
        Assertions.assertTrue(node.isConnected());

        node.shutdown();
    }

    @Test
    @DisplayName("initialize → 연결 실패 시 상태")
    void test4() {
        EchoProtocolNode node = new EchoProtocolNode("echo-node", Map.of("port", 12345));
        node.initialize();

        Assertions.assertEquals(ConnectionState.ERROR, node.getConnectionState());
        Assertions.assertFalse(node.isConnected());

        node.shutdown();
    }

    @Test
    @DisplayName("shutdown → DISCONNECTED")
    void test5() {
        EchoProtocolNode node = new EchoProtocolNode("echo-node", Map.of());
        node.initialize();
        Assertions.assertEquals(ConnectionState.CONNECTED, node.getConnectionState());

        node.shutdown();
        Assertions.assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
        Assertions.assertFalse(node.isConnected());
    }

    @Test
    @DisplayName("isConnected 반환값")
    void test6() {
        EchoProtocolNode node = new EchoProtocolNode("echo-node", Map.of());
        Assertions.assertFalse(node.isConnected());

        node.initialize();
        Assertions.assertTrue(node.isConnected());

        node.shutdown();
        Assertions.assertFalse(node.isConnected());
    }

    @Test
    @DisplayName("재연결 시도")
    void test7() throws InterruptedException {
        AtomicInteger connectAttempts = new AtomicInteger(0);
        ProtocolNode retryNode = new ProtocolNode("retry-Node", Map.of("reconnectIntervalMs", 100L)) {
            @Override
            protected void onProcess(Message message) {}

            @Override
            protected void connect() throws Exception {
                connectAttempts.incrementAndGet();
                throw new Exception("의도적인 강제 연결 실패");
            }

            @Override
            protected void disconnect() {}
        };
        retryNode.initialize();
        Thread.sleep(350);

        int actualAttempts = connectAttempts.get();
        Assertions.assertTrue(actualAttempts >= 3);

        retryNode.shutdown();
    }
}
