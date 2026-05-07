package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
import com.fbp.engine.node.TransformNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class FlowTest {
    @Test
    @DisplayName("노드 등록")
    void test1() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new PrintNode("p1"));
        Assertions.assertTrue(flow.getNodes().containsKey("p1"));
    }

    @Test
    @DisplayName("메서드 체이닝")
    void test2() {
        Flow flow = new Flow("f1", "f1");
        Assertions.assertDoesNotThrow(() -> flow.addNode(new TimerNode("t1", 100))
                .addNode(new PrintNode("p1"))
                .connect("t1","out","p1","in"));
    }

    @Test
    @DisplayName("정상 연결")
    void test3() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new TimerNode("t1", 100)).addNode(new PrintNode("p1"));
        Assertions.assertEquals(0, flow.getConnections().size());

        flow.connect("t1","out","p1","in");
        Assertions.assertEquals(1, flow.getConnections().size());
        Assertions.assertEquals("t1:out->p1:in", flow.getConnections().getFirst().getId());
    }

    @Test
    @DisplayName("존재하지 않는 소스 노드 ID")
    void test4() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new PrintNode("p1"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> flow.connect("wrong-source","out","p1","in"));
    }

    @Test
    @DisplayName("존재하지 않는 대상 노드 ID")
    void test5() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new PrintNode("p1"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> flow.connect("f1","out","wrong-target","in"));
    }

    @Test
    @DisplayName("존재하지 않는 소스 포트")
    void test6() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new PrintNode("p1"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> flow.connect("f1","wrong-port","p1","in"));
    }

    @Test
    @DisplayName("존재하지 않는 대상 포트")
    void test7() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new PrintNode("p1"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> flow.connect("f1","out","p1","wrong-port"));
    }

    @Test
    @DisplayName("validate — 빈 Flow")
    void test8() {
        Flow flow = new Flow("f1", "f1");
        List<String> errors = flow.validate();

        Assertions.assertFalse(errors.isEmpty());
        Assertions.assertTrue(errors.getFirst().contains("없습니다"));
    }

    @Test
    @DisplayName("validate — 정상 Flow")
    void test9() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new TimerNode("t1", 100))
                .addNode(new PrintNode("p1"))
                .connect("t1","out","p1","in");
        Assertions.assertTrue(flow.validate().isEmpty());
    }

    static class MockNode extends AbstractNode {
        boolean initCalled = false;
        boolean shutdownCalled = false;

        public MockNode(String id) {
            super(id);
        }

        @Override public void initialize() {initCalled = true;}
        @Override public void shutdown() {shutdownCalled = true;}
        @Override protected void onProcess(Message message) {}
    }

    @Test
    @DisplayName("initialize — 전체 호출")
    void test10() {
        Flow flow = new Flow("f1", "f1");
        MockNode n1 = new MockNode("n1");
        MockNode n2 = new MockNode("n2");
        flow.addNode(n1).addNode(n2);

        flow.initialize();
        Assertions.assertTrue(n1.initCalled && n2.initCalled);
    }
    @Test
    @DisplayName("shutdown — 전체 호출")
    void test11() {
        Flow flow = new Flow("f1", "f1");
        MockNode n1 = new MockNode("n1");
        flow.addNode(n1);
        flow.shutdown();
        Assertions.assertTrue(n1.shutdownCalled);
    }
    @Test
    @DisplayName("순환 참조 탐지")
    void test12() {
        Flow flow = new Flow("f1", "f1");
        flow.addNode(new TransformNode("A", msg -> msg))
                .addNode(new TransformNode("B", msg -> msg))
                .addNode(new TransformNode("C", msg -> msg));

        flow.connect("A", "out", "B","in")
                .connect("B","out","C","in")
                .connect("C","out","A","in");

        List<String> errors = flow.validate();
        Assertions.assertFalse(errors.isEmpty());

        boolean hasCycleError = errors.stream().anyMatch(error -> error.contains("순환 연결") || error.contains("Cycle"));
        Assertions.assertTrue(hasCycleError);

    }
}
