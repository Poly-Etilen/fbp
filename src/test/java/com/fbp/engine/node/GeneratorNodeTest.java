package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeneratorNodeTest {

    static class MockTargetNode extends AbstractNode {
        public MockTargetNode(String id) {
            super(id);
            addInputPort("in");
        }
        @Override protected void onProcess(Message msg) {}
    }

    private Connection setupGeneratorAndGetConnection(AbstractNode generator) {
        Flow flow = new Flow("test-flow", "테스트 플로우")
                .addNode(generator)
                .addNode(new MockTargetNode("target"))
                .connect(generator.getId(), "out", "target", "in");

        return flow.getConnections().get(0).getConnection();
    }

    @Test
    @DisplayName("generate 메시지 생성")
    void test1() throws InterruptedException{
        GeneratorNode node = new GeneratorNode("generator-1");
        Connection connection = setupGeneratorAndGetConnection(node);
        node.generate("key", "value");

        Assertions.assertNotNull(connection.poll());
    }

    @Test
    @DisplayName("메시지 내용 확인")
    void test2() throws InterruptedException {
        GeneratorNode node = new GeneratorNode("generator-1");
        Connection connection = setupGeneratorAndGetConnection(node);

        node.generate("sensorType", "temperature");
        Message outMsg = connection.poll();

        Assertions.assertNotNull(outMsg);
        Assertions.assertEquals("temperature", outMsg.get("sensorType"));
    }

    @Test
    @DisplayName("OutputPort 조회")
    void test3() {
        GeneratorNode node = new GeneratorNode("generator-1");
        Assertions.assertDoesNotThrow(() -> node.getOutputPort("out"));
        Assertions.assertNotNull(node.getOutputPort("out"));
    }

    @Test
    @DisplayName("다수 generate 호출")
    void test4() throws InterruptedException {
        GeneratorNode node = new GeneratorNode("generator-1");
        Connection connection = setupGeneratorAndGetConnection(node);

        node.generate("seq", 1);
        Assertions.assertEquals(1, (int) connection.poll().get("seq"));

        node.generate("seq", 2);
        Assertions.assertEquals(2, (int) connection.poll().get("seq"));

        node.generate("seq", 3);
        Assertions.assertEquals(3, (int) connection.poll().get("seq"));
    }
}
