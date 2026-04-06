package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.portImpl.InputPortImpl;
import com.fbp.engine.core.portImpl.OutputPortImpl;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
public class App {
    public static void main(String[] args) {
        TimerNode timer = new TimerNode("timer-1", 1000);
        FilterNode filter = new FilterNode("filter-1", "FBP");
        PrintNode printNode = new PrintNode("printer-1");

        OutputPortImpl timerOut = new OutputPortImpl();
        InputPortImpl filterIn = new InputPortImpl();
        OutputPortImpl filterOut = new OutputPortImpl();
        InputPortImpl printIn = new InputPortImpl();

        timer.addOutputPort("output", timerOut);
        filter.addInputPort("input", filterIn);
        filter.addOutputPort("output", filterOut);

        printNode.addInputPort("input", printIn);

        Connection conn1 = new Connection();
        timerOut.connect(conn1);
        filterIn.connect(conn1);

        Connection conn2 = new Connection();
        filterOut.connect(conn2);
        printIn.connect(conn2);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        log.info("--- FBP Engine Started ---");
        executor.submit(timer);
        executor.submit(filter);
        executor.submit(printNode);
    }
}
