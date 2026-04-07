package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class GenerateNodeTest {
    @Test
    @DisplayName("generate 메시지 생성")
    void test1() {
        GeneratorNode generator = new GeneratorNode("gen");
        Connection conn = new Connection();
        generator.getOutputPort().connect(conn);

        generator.generate("temperature", 25.5);
        Assertions.assertEquals(1, conn.getBufferSize());
    }

    @Test
    @DisplayName("메시지 내용 확인")
    void test2() {
        GeneratorNode generator = new GeneratorNode("gen");
        Connection conn = new Connection();
        generator.getOutputPort().connect(conn);

        final Message[] captured = new Message[1];
        conn.setTarget(new InputPort() {
            @Override
            public String getName() {
                return "mock-in";
            }
            @Override
            public void receive(Message message) {
                captured[0] = message;
            }
        });
        generator.generate("humidity", 60.0);
        Assertions.assertNotNull(captured[0]);
        Assertions.assertEquals(60.0, (double) captured[0].get("humidity"), "페이로드에 지정한 key-value가 포함되어야 함");
    }

    @Test
    @DisplayName("OutputPort 조회")
    void test3() {
        GeneratorNode generator = new GeneratorNode("gen");
        Assertions.assertNotNull(generator.getOutputPort(), "getOutputPort()는 Null이어서는 안됨.");
    }

    @Test
    @DisplayName("다수 generate 호출")
    void test4() {
        GeneratorNode generator = new GeneratorNode("gen");
        Connection connection = new Connection();
        List<Message> received = new ArrayList<>();

        connection.setTarget(new InputPort() {
            @Override
            public String getName() {return "mock-in";}
            @Override
            public void receive(Message message) {received.add(message);}
        });

        generator.getOutputPort().connect(connection);
        generator.generate("seq", 1);
        generator.generate("seq", 2);
        generator.generate("seq", 3);

        Assertions.assertEquals(3, received.size());
        Assertions.assertEquals(1, (int) received.get(0).get("seq"));
        Assertions.assertEquals(2, (int) received.get(1).get("seq"));
        Assertions.assertEquals(3, (int) received.get(2).get("seq"));
    }
}
