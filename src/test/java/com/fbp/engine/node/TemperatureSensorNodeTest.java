package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class TemperatureSensorNodeTest {
    static class MockTargetNode extends AbstractNode {
        public MockTargetNode(String id) {
            super(id);
            addInputPort("in");
        }
        @Override protected void onProcess(Message msg) {}
    }

    private Connection setupSensorAndGetConnection(TemperatureSensorNode sensor) {
        Flow flow = new Flow("test-flow", "테스트 플로우")
                .addNode(sensor)
                .addNode(new MockTargetNode("target"))
                .connect(sensor.getId(), "out", "target", "in");

        return flow.getConnections().getFirst().getConnection();
    }

    @Test
    @DisplayName("온도 범위 확인")
    void test1() throws InterruptedException {
        TemperatureSensorNode sensor = new TemperatureSensorNode("t1", 15.0, 45.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Message message = connection.poll();

        Assertions.assertNotNull(message);
        double temperature = (double) message.get("temperature");
        Assertions.assertTrue(temperature >= 15.0 && temperature <= 45.0);
    }

    @Test
    @DisplayName("필수 키 포함")
    void test2() throws InterruptedException {
        TemperatureSensorNode sensor = new TemperatureSensorNode("t1", 15.0, 45.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Message outMsg = connection.poll();

        Assertions.assertNotNull(outMsg.get("sensorId"));
        Assertions.assertNotNull(outMsg.get("temperature"));
        Assertions.assertNotNull(outMsg.get("unit"));
        Assertions.assertNotNull(outMsg.get("timestamp"));
    }

    @Test
    @DisplayName("sensorId 일치")
    void test3() throws InterruptedException {
        TemperatureSensorNode sensor = new TemperatureSensorNode("temp-sensor-99", 15.0, 45.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Message outMsg = connection.poll();

        Assertions.assertEquals("temp-sensor-99", outMsg.get("sensorId"));
    }

    @Test
    @DisplayName("트리거마다 생성")
    void test4() throws InterruptedException {
        TemperatureSensorNode sensor = new TemperatureSensorNode("t1", 15.0, 45.0);
        Connection connection = setupSensorAndGetConnection(sensor);

        sensor.process(new Message(Map.of()));
        Assertions.assertNotNull(connection.poll());

        sensor.process(new Message(Map.of()));
        Assertions.assertNotNull(connection.poll());

        sensor.process(new Message(Map.of()));
        Assertions.assertNotNull(connection.poll());
    }
}
