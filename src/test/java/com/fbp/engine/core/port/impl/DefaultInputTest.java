package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class DefaultInputTest {
    @Test
    @DisplayName("receive 시 owner 호출")
    void test1() {
        final boolean[] isProcess = {false};
        Node mockOwner = new Node() {
            @Override
            public String getId() {
                return "mock";
            }

            @Override
            public void process(Message message) {
                isProcess[0] = true;
            }
        };

        DefaultInputPort in = new DefaultInputPort("in-port", mockOwner);
        in.receive(new Message(Map.of()));
        Assertions.assertTrue(isProcess[0], "owner 노드의 process()가 호출되어야 함.");
    }

    @Test
    @DisplayName("포트 이름 확인")
    void test2() {
        Node mockOwner = new Node() {
            @Override
            public String getId() {return "mock";}
            @Override
            public void process(Message message) {}
        };
        DefaultInputPort in = new DefaultInputPort("test-in", mockOwner);
        Assertions.assertEquals("test-in", in.getName());
    }
}
