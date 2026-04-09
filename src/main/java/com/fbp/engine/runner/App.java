package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
public class App {
    public static void main(String[] args) throws InterruptedException{
        Flow flow = new Flow("split-flow");
        flow.addNode(new TimerNode("timer", 1000))
                .addNode(new SplitNode("split", "tick", 3.0))
                .addNode(new PrintNode("print-warn"))
                .addNode(new PrintNode("print-normal"))
                .connect("timer", "out", "split", "in")
                .connect("split", "match", "print-warn", "in")
                .connect("split", "mismatch", "print-normal", "in");

        List<Thread> engineThreads = new ArrayList<>();

        for (Flow.FlowConnection fc : flow.getConnections()) {
            AbstractNode targetNode = flow.getNodes().get(fc.getTargetNodeId());
            Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Message msg = fc.getConnection().poll();
                        if (msg != null) {
                            targetNode.process(msg);
                        } else {
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "worker-" + fc.getId());

            engineThreads.add(thread);
            thread.start();
        }

        flow.initialize();
        Thread.sleep(7000);
        flow.shutdown();
        engineThreads.forEach(Thread::interrupt);
    }
}
