package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.portImpl.OutputPortImpl;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
public class App {
    public static void main(String[] args) {
        OutputPortImpl outputPort = new OutputPortImpl();
        GeneratorNode generator = new GeneratorNode("generator-1", outputPort);
        PrintNode printNode = new PrintNode("printer-1");

        Connection conn = new Connection();
        // outputPort를 Connection과 연결함.
        outputPort.connect(conn);
        printNode.getInputPort().connect(conn);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        log.info("--- FBP Engine Started ---");
        executor.submit(generator);
        executor.submit(printNode);
    }
}
