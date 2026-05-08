package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FilterNodeTest {
    @Test
    @DisplayName("조건 만족 → send 호출")
    void test1() throws InterruptedException {
        FilterNode filter = new FilterNode("filter", "temperature", 30.0);
        Connection conn = new LocalConnection();
        filter.getOutputPort("out").connect(conn);

        Message passMsg = new Message(Map.of("temperature", 35.0));
        filter.process(passMsg);

        Message receiveMsg = conn.poll();
        Assertions.assertNotNull(receiveMsg);
        Assertions.assertEquals(35.0, receiveMsg.get("temperature"));
    }

    @Test
    @DisplayName("조건 미달 → 차단")
    void test2() {
        FilterNode filter = new FilterNode("filter", "temperature", 30.0);
        Connection conn = new LocalConnection();
        filter.getOutputPort("out").connect(conn);

        Message blockMsg = new Message(Map.of("temperature", 25.0));
        filter.process(blockMsg);

        Assertions.assertEquals(0, conn.getBufferSize());
    }

    @Test
    @DisplayName("포트 구성 확인")
    void test3() {
        FilterNode filter = new FilterNode("filter", "temperature", 30.0);
        Assertions.assertNotNull(filter.getInputPort("in"));
        Assertions.assertNotNull(filter.getOutputPort("out"));
    }
}
