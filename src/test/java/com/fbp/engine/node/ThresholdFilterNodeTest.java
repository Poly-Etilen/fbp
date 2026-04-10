package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ThresholdFilterNodeTest {
    static class MockTargetNode extends AbstractNode {
        public MockTargetNode(String id) {
            super(id);
            addInputPort("in");
        }
        @Override protected void onProcess(Message msg) {}
    }

    static class FilterConnections {
        Connection alertConn;
        Connection normalConn;
    }

    private FilterConnections setupFilterAndGetConnections(ThresholdFilterNode filter) {
        Flow flow = new Flow("test-flow")
                .addNode(filter)
                .addNode(new MockTargetNode("target-alert"))
                .addNode(new MockTargetNode("target-normal"))
                .connect(filter.getId(), "alert", "target-alert", "in")
                .connect(filter.getId(), "normal", "target-normal", "in");

        FilterConnections conns = new FilterConnections();
        for (Flow.FlowConnection fc : flow.getConnections()) {
            if (fc.getTargetNodeId().equals("target-alert")) {
                conns.alertConn = fc.getConnection();
            } else if (fc.getTargetNodeId().equals("target-normal")) {
                conns.normalConn = fc.getConnection();
            }
        }
        return conns;
    }

    @Test
    @DisplayName("초과 → alert 포트")
    void test1() throws InterruptedException{
        ThresholdFilterNode filter = new ThresholdFilterNode("f1", "temperature", 30.0);
        FilterConnections conns = setupFilterAndGetConnections(filter);

        filter.process(new Message(Map.of("temperature", 35.5)));

        Message alertMsg = conns.alertConn.poll();
        Assertions.assertNotNull(alertMsg);
        Assertions.assertEquals(35.5, alertMsg.get("temperature"));
    }

    @Test
    @DisplayName("이하 → normal 포트")
    void test2() throws InterruptedException{
        ThresholdFilterNode filter = new ThresholdFilterNode("f1", "temperature", 30.0);
        FilterConnections conns = setupFilterAndGetConnections(filter);

        filter.process(new Message(Map.of("temperature", 25.0)));

        Message normalMsg = conns.normalConn.poll();
        Assertions.assertNotNull(normalMsg);
        Assertions.assertEquals(25.0, normalMsg.get("temperature"));
    }

    @Test
    @DisplayName("경계값 (정확히 같은 값)")
    void test3() throws InterruptedException{
        ThresholdFilterNode filter = new ThresholdFilterNode("f1", "temperature", 30.0);
        FilterConnections conns = setupFilterAndGetConnections(filter);

        filter.process(new Message(Map.of("temperature", 30.0)));

        Message normalMsg = conns.normalConn.poll();
        Assertions.assertNotNull(normalMsg);
        Assertions.assertEquals(30.0, normalMsg.get("temperature"));
    }

    @Test
    @DisplayName("키 없는 메시지")
    void test4() throws InterruptedException {
        ThresholdFilterNode filter = new ThresholdFilterNode("f1", "temperature", 30.0);

        Assertions.assertDoesNotThrow(() -> filter.process(new Message(Map.of("humidity", 80.0))));
    }

    @Test
    @DisplayName("양쪽 동시 검증")
    void test5() throws InterruptedException {
        ThresholdFilterNode filter = new ThresholdFilterNode("f1", "temperature", 30.0);
        FilterConnections conns = setupFilterAndGetConnections(filter);

        filter.process(new Message(Map.of("temperature", 35.0)));
        Assertions.assertEquals(35.0, conns.alertConn.poll().get("temperature"));

        filter.process(new Message(Map.of("temperature", 20.0)));
        Assertions.assertEquals(20.0, conns.normalConn.poll().get("temperature"));

        filter.process(new Message(Map.of("temperature", 30.0)));
        Assertions.assertEquals(30.0, conns.normalConn.poll().get("temperature"));

        filter.process(new Message(Map.of("temperature", 45.0)));
        Assertions.assertEquals(45.0, conns.alertConn.poll().get("temperature"));
    }
}
