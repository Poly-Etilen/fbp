package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class HumiditySensorNodeTest {

    static class MockTargetNode extends AbstractNode {
        public MockTargetNode(String id) {
            super(id);
            addInputPort("in");
        }
        @Override protected void onProcess(Message msg) {}
    }

    private Connection setupSensorAndGetConnection(HumiditySensorNode sensor) {
        Flow flow = new Flow("test-flow")
                .addNode(sensor)
                .addNode(new MockTargetNode("target"))
                .connect(sensor.getId(), "out", "target", "in");

        return flow.getConnections().get(0).getConnection();
    }

    @Test
    @DisplayName("습도 범위 확인")
    void test1() throws InterruptedException{
        HumiditySensorNode sensor = new HumiditySensorNode("h1", 30.0, 90.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));

        Message message = connection.poll();
        Assertions.assertNotNull(message);

        double humidity = (double)  message.get("humidity");
        Assertions.assertTrue(humidity >= 30.0 && humidity <= 90.0);
    }

    @Test
    @DisplayName("필수 키 포함")
    void test2() throws InterruptedException {
        HumiditySensorNode sensor = new HumiditySensorNode("h1", 30.0, 90.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Message message = connection.poll();

        Assertions.assertNotNull(message.get("sensorId"));
        Assertions.assertNotNull(message.get("humidity"));
        Assertions.assertNotNull(message.get("unit"));
    }

    @Test
    @DisplayName("sensorId 일치")
    void test3() throws InterruptedException {
        HumiditySensorNode sensor = new HumiditySensorNode("my-sensor", 30.0, 90.0);
        Connection outConn = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Message outMsg = outConn.poll();

        Assertions.assertEquals("my-sensor", outMsg.get("sensorId"));
    }

    @Test
    @DisplayName("트리거마다 생성")
    void test4() throws InterruptedException {
        HumiditySensorNode sensor = new HumiditySensorNode("h1", 30.0, 90.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Assertions.assertNotNull(connection.poll());

        sensor.process(new Message(Map.of()));
        Assertions.assertNotNull(connection.poll());

        sensor.process(new Message(Map.of()));
        Assertions.assertNotNull(connection.poll());

    }
}
