package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class AbstractNodeTest {

    static class Dummy extends AbstractNode {
        boolean onProcessCalled = false;
        public Dummy(String id) {super(id);}
        public void triggerAddInputPort(String name){addInputPort(name);}
        public void triggerAddOutputPort(String name){addOutputPort(name);}
        public void triggerSend(String portName, Message msg) {send(portName,msg);}
        @Override protected void onProcess(Message msg) {onProcessCalled = true;}
    }
    @Test
    @DisplayName("getId 반환")
    void test1() {
        Dummy node = new Dummy("node-1");
        Assertions.assertEquals("node-1", node.getId());
    }

    @Test
    @DisplayName("addInputPort 등록")
    void test2() throws InterruptedException {
        Dummy node = new Dummy("node-1");
        node.triggerAddInputPort("in");
        Assertions.assertNotNull(node.getInputPort("in"));
    }

    @Test
    @DisplayName("addOutputPort 등록")
    void test3() {
        Dummy node = new Dummy("node-1");
        node.triggerAddOutputPort("out");
        Assertions.assertNotNull(node.getOutputPort("out"));
    }

    @Test
    @DisplayName("미등록 포트 조회")
    void test4() {
        Dummy node = new Dummy("node-1");
        Assertions.assertNull(node.getInputPort("annonymous"));
    }

    @Test
    @DisplayName("process → onProcess 호출")
    void test5() {
        Dummy node = new Dummy("node-1");
        Assertions.assertFalse(node.onProcessCalled);
        node.process(new Message(Map.of()));
        Assertions.assertTrue(node.onProcessCalled);
    }

    @Test
    @DisplayName("send로 메시지 전달")
    void test6() throws InterruptedException {
        Dummy node = new Dummy("node-1");
        node.triggerAddOutputPort("out");

        Connection conn = new Connection();
        node.getOutputPort("out").connect(conn);

        Message msg = new Message(Map.of("data", "hello"));
        node.triggerSend("out", msg);

        Assertions.assertEquals(msg, conn.poll());
    }
}
