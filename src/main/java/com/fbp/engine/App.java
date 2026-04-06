package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.portImpl.InputPortImpl;
import com.fbp.engine.core.portImpl.OutputPortImpl;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;

import java.util.LinkedList;
import java.util.Map;


public class App {
    public static void main(String[] args) {
        OutputPortImpl outputPort = new OutputPortImpl();
        GeneratorNode generator = new GeneratorNode("generator-1", outputPort);
        PrintNode printNode = new PrintNode("printer-1");

        Connection conn = new Connection(new LinkedList<>());
        // outputPort를 Connection과 연결함.
        outputPort.connect(conn);

        // Connection에서 InputPort를 설정함
        conn.setTarget(printNode.getInputPort());

        Message msg = new Message("message-1", Map.of("data", "Hello"), System.currentTimeMillis());

        generator.process(msg);
    }
}
