package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlowEngineTest {
    static class MockNode extends AbstractNode {
        public MockNode(String name) {
            super(name);
        }
        @Override protected void onProcess(Message msg) {}
    }
    private Flow createValidFlow(String id) {
        return new Flow(id).addNode(new MockNode(id + "-node"));
    }
    @Test
    @DisplayName("초기 상태")
    void test1() {
        FlowEngine engine = new FlowEngine();
        Assertions.assertEquals(FlowEngine.State.INITIALIZED, engine.getState());
    }

    @Test
    @DisplayName("플로우 등록")
    void test2() {
        FlowEngine engine = new FlowEngine();
        engine.register(createValidFlow("f1"));
        Assertions.assertTrue(engine.getFlows().containsKey("f1"));
    }

    @Test
    @DisplayName("startFlow 정상")
    void test3() {
        FlowEngine engine = new FlowEngine();
        engine.register(createValidFlow("f1"));

        engine.startFlow("f1");

        Assertions.assertEquals(FlowEngine.State.RUNNING, engine.getState());
        Assertions.assertEquals(Flow.FlowState.RUNNING, engine.getFlows().get("f1").getState());
    }

    @Test
    @DisplayName("startFlow - 없는 ID")
    void test4() {
        FlowEngine engine = new FlowEngine();
        Assertions.assertThrows(IllegalArgumentException.class, () -> engine.startFlow("wrong-id"));
    }

    @Test
    @DisplayName("startFlow - 유효성 실패")
    void test5() {
        FlowEngine engine = new FlowEngine();
        engine.register(new Flow("empty-flow"));

        Assertions.assertThrows(IllegalStateException.class, () -> engine.startFlow("empty-flow"));
    }

    @Test
    @DisplayName("stopFlow 정상")
    void test6() {
        FlowEngine engine = new FlowEngine();
        engine.register(createValidFlow("f1"));

        engine.startFlow("f1");
        engine.stopFlow("f1");

        Assertions.assertEquals(Flow.FlowState.STOPPED, engine.getFlows().get("f1").getState());
    }

    @Test
    @DisplayName("shutdown 전체")
    void test7() {
        FlowEngine engine = new FlowEngine();
        engine.register(createValidFlow("f1"));
        engine.register(createValidFlow("f2"));

        engine.startFlow("f1");
        engine.startFlow("f2");

        engine.shutdown();

        Assertions.assertEquals(FlowEngine.State.STOPPED, engine.getState());
        Assertions.assertEquals(Flow.FlowState.STOPPED, engine.getFlows().get("f1").getState());
        Assertions.assertEquals(Flow.FlowState.STOPPED, engine.getFlows().get("f2").getState());
    }

    @Test
    @DisplayName("다중 플로우 독립 동작")
    void test8() {
        FlowEngine engine = new FlowEngine();
        engine.register(createValidFlow("f1"));
        engine.register(createValidFlow("f2"));

        engine.startFlow("f1");
        engine.startFlow("f2");

        engine.stopFlow("f1");

        Assertions.assertEquals(Flow.FlowState.STOPPED, engine.getFlows().get("f1").getState());
        Assertions.assertEquals(Flow.FlowState.RUNNING, engine.getFlows().get("f2").getState());
    }

    @Test
    @DisplayName("listFlows 출력")
    void test9() {
        FlowEngine engine = new FlowEngine();
        engine.register(createValidFlow("f1"));
        engine.register(createValidFlow("f2"));

        engine.startFlow("f1");
        Assertions.assertDoesNotThrow(engine::listFlows);
    }
}
