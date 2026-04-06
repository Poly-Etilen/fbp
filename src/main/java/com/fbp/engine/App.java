package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.portImpl.DefaultInputPort;
import com.fbp.engine.core.portImpl.DefaultOutputPort;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.LogNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
public class App {
    public static void main(String[] args) {
        GeneratorNode generator = new GeneratorNode("generator-1", 1000);
        FilterNode filter = new FilterNode("filter-1", "FBP");
        LogNode logNode = new LogNode("printer-1");

        DefaultOutputPort timerOut = new DefaultOutputPort();
        DefaultInputPort filterIn = new DefaultInputPort();
        DefaultOutputPort filterOut = new DefaultOutputPort();
        DefaultInputPort printIn = new DefaultInputPort();

        generator.addOutputPort("output", timerOut);
        filter.addInputPort("input", filterIn);
        filter.addOutputPort("output", filterOut);
        logNode.addInputPort("input", printIn);

        Connection conn1 = new Connection();
        timerOut.connect(conn1);
        filterIn.connect(conn1);

        Connection conn2 = new Connection();
        filterOut.connect(conn2);
        printIn.connect(conn2);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        log.info("--- FBP Engine Started ---");
        executor.submit(generator);
        executor.submit(filter);
        executor.submit(logNode);
    }
}
