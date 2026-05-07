package com.fbp.engine.runner;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class App {
    public static void main(String[] args) throws InterruptedException{
        FlowEngine engine = new FlowEngine();

        Flow flow = new Flow("final-scenario", "라스트 시나리오")
                .addNode(new TimerNode("timer", 10000))
                .addNode(new TemperatureSensorNode("sensor", 15.0, 45.0))
                .addNode(new ThresholdFilterNode("filter", "temperature", 30.0))
                .addNode(new AlertNode("alert"))
                .addNode(new LogNode("log"))
                .addNode(new FileWriterNode("file-writer", "final_normal_temp.txt"))

                .connect("timer", "out", "sensor", "trigger")
                .connect("sensor", "out", "filter", "in")
                .connect("filter", "alert", "alert", "in")

                .connect("filter", "normal", "log", "in")
                .connect("filter", "normal", "file-writer", "in");

        engine.register(flow);

        List<Thread> workers = new ArrayList<>();
        for (Flow.FlowConnection fc : flow.getConnections()) {
            AbstractNode targetNode = flow.getNodes().get(fc.getTargetNodeId());
            Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Message msg = fc.getConnection().poll();
                        if (msg != null && flow.getState() == Flow.FlowState.RUNNING) {
                            targetNode.process(msg);
                        } else {
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "worker-" + fc.getId());
            workers.add(thread);
            thread.start();

            log.info("온도 모니터링 시스템 가동 시작");
            engine.startFlow("final-scenario");
            Thread.sleep(10000);

            log.info("종료 요청");
            engine.shutdown();
            workers.forEach(Thread::interrupt);
        }
    }
}
