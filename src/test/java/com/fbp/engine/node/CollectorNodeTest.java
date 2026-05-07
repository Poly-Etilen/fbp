package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CollectorNodeTest {

    @Test
    @DisplayName("메시지 수집")
    public void test1() {
        CollectorNode node = new CollectorNode("collector");
        node.process(new Message(Map.of("data", "test1")));
        Assertions.assertEquals(1, node.getCollected().size());
        Assertions.assertEquals("test1", node.getCollected().getFirst().get("data"));
    }

    @Test
    @DisplayName("수집 순서 보존")
    public void test2() {
        CollectorNode node = new CollectorNode("collector");
        node.process(new Message(Map.of("seq", 1)));
        node.process(new Message(Map.of("seq", 2)));
        node.process(new Message(Map.of("seq", 3)));

        Assertions.assertEquals(3, node.getCollected().size());
        Assertions.assertEquals(1, (int) node.getCollected().getFirst().get("seq"));
        Assertions.assertEquals(2, (int) node.getCollected().get(1).get("seq"));
        Assertions.assertEquals(3, (int) node.getCollected().get(2).get("seq"));
    }

    @Test
    @DisplayName("초기 상태 빈 리스트")
    public void test3() {
        CollectorNode node = new CollectorNode("collector");
        Assertions.assertTrue(node.getCollected().isEmpty());
    }

    @Test
    @DisplayName("InputPort 존재")
    public void test4() {
        CollectorNode node = new CollectorNode("collector");
        Assertions.assertDoesNotThrow(() -> node.getOutputPort("in"));
        Assertions.assertNotNull(node.getInputPort("in"));
    }

    @Test
    @DisplayName("파이프라인 연결 검증")
    public void test5() throws InterruptedException{
        TemperatureSensorNode sensor = new TemperatureSensorNode("sensor", 10.0, 20.0);
        CollectorNode collector = new CollectorNode("collector");

        Flow flow = new Flow("test-flow", "테스트 플로우")
                .addNode(sensor)
                .addNode(collector)
                .connect("sensor", "out", "collector", "in");

        Connection conn = flow.getConnections().getFirst().getConnection();

        sensor.process(new Message(Map.of()));
        sensor.process(new Message(Map.of()));
        sensor.process(new Message(Map.of()));

        collector.process(conn.poll());
        collector.process(conn.poll());
        collector.process(conn.poll());

        Assertions.assertEquals(3, collector.getCollected().size());
    }
}
