package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FilterNodeTest {
    @Test
    @DisplayName("조건 만족 시 통과")
    void test1() {
        FilterNode filter = new FilterNode("filter", "temp", 30.0);
        Connection conn = new Connection();
        filter.getOutputPort().connect(conn);

        filter.process(new Message(Map.of("temp", 35.0)));
        Assertions.assertEquals(1, conn.getBufferSize(), "조건을 만족하여 통과됨");
    }

    @Test
    @DisplayName("조건 미달 시 차단")
    void test2() {
        FilterNode filter = new FilterNode("filter", "temp", 30.0);
        Connection conn = new Connection();
        filter.getOutputPort().connect(conn);

        filter.process(new Message(Map.of("temp", 25.0)));
        Assertions.assertEquals(0, conn.getBufferSize(), "조건 미달로 차단됨");
    }

    @Test
    @DisplayName("경계값 처리")
    void test3() {
        FilterNode filter = new FilterNode("filter", "temp", 30.0);
        Connection conn = new Connection();
        filter.getOutputPort().connect(conn);

        filter.process(new Message(Map.of("temp", 30.0)));
        Assertions.assertEquals(1, conn.getBufferSize(), "30.0은 통과되어야함");
    }

    @Test
    @DisplayName("키 없는 메시지")
    void test4() {
        FilterNode filter = new FilterNode("filter", "temp", 30.0);

        Assertions.assertDoesNotThrow(() -> filter.process(new Message(Map.of("otherKey", 30.0))));
    }
}
