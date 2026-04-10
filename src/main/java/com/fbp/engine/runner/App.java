package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;
import java.util.*;


@Slf4j
public class App {
    // 9-4
//    public static void main(String[] args) throws InterruptedException{
//        FlowEngine flowEngine = new FlowEngine();
//
//        Flow flow = new Flow("temperature-monitoring")
//                .addNode(new TimerNode("timer", 1000))
//                .addNode(new TemperatureSensorNode("sensor", 15.0, 45.0))
//                .addNode(new ThresholdFilterNode("filter", "temperature", 30.0))
//                .addNode(new AlertNode("alert"))
//                .addNode(new LogNode("log"))
//                .connect("timer", "out", "sensor", "trigger")
//                .connect("sensor", "out", "filter", "in")
//                .connect("filter", "alert", "alert", "in")
//                .connect("filter", "normal", "log", "in");
//
//        flowEngine.register(flow);
//
//        List<Thread> workers = new ArrayList<>();
//        for (Flow f : flowEngine.getFlows().values()) {
//            for (Flow.FlowConnection fc : f.getConnections()) {
//                AbstractNode targetNode = f.getNodes().get(fc.getTargetNodeId());
//                Thread thread = new Thread(() -> {
//                    while (!Thread.currentThread().isInterrupted()) {
//                        try {
//                            Message msg = fc.getConnection().poll();
//                            if (msg != null && f.getState() == Flow.FlowState.RUNNING) {
//                                targetNode.process(msg);
//                            } else {
//                                Thread.sleep(10);
//                            }
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                        }
//                    }
//                }, "worker-" + fc.getId());
//                workers.add(thread);
//                thread.start();
//            }
//        }
//
//        System.out.println("IoT 온도 모니터링 시스템 가동 시작");
//        flowEngine.startFlow("temperature-monitoring");
//        Thread.sleep(10000);
//
//        System.out.println("종료 요청");
//        flowEngine.shutdown();
//        workers.forEach(Thread::interrupt);
//    }

    // 9-5
//    public static void main(String[] args) throws InterruptedException{
//        FlowEngine flowEngine = new FlowEngine();
//
//        Flow flow = new Flow("dual-monitoring")
//                .addNode(new TimerNode("timer", 1000))
//
//                .addNode(new TemperatureSensorNode("t-sensor", 15, 45))
//                .addNode(new ThresholdFilterNode("t-filter", "temperature", 30.0))
//                .addNode(new AlertNode("t-alert"))
//
//                .addNode(new HumiditySensorNode("h-sensor", 30, 90))
//                .addNode(new ThresholdFilterNode("h-filter", "humidity", 70.0))
//                .addNode(new AlertNode("h-alert"))
//
//                .addNode(new LogNode("log"))
//                .connect("timer", "out", "t-sensor", "trigger")
//                .connect("timer", "out", "h-sensor", "trigger")
//
//                .connect("t-sensor", "out", "t-filter", "in")
//                .connect("t-filter", "alert", "t-alert", "in")
//                .connect("t-filter", "normal", "log", "in")
//
//                .connect("h-sensor", "out", "h-filter", "in")
//                .connect("h-filter", "alert", "h-alert", "in")
//                .connect("h-filter", "normal", "log", "in");
//
//        flowEngine.register(flow);
//
//        List<Thread> workers = new ArrayList<>();
//        for (Flow f : flowEngine.getFlows().values()) {
//            for (Flow.FlowConnection fc : f.getConnections()) {
//                AbstractNode targetNode = f.getNodes().get(fc.getTargetNodeId());
//                Thread thread = new Thread(() -> {
//                    while (!Thread.currentThread().isInterrupted()) {
//                        try {
//                            Message msg = fc.getConnection().poll();
//                            if (msg != null && f.getState() == Flow.FlowState.RUNNING) {
//                                targetNode.process(msg);
//                            } else {
//                                Thread.sleep(10);
//                            }
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                        }
//                    }
//                }, "worker-" + fc.getId());
//                workers.add(thread);
//                thread.start();
//            }
//        }
//
//        System.out.println("IoT 온도 모니터링 시스템 가동 시작");
//        flowEngine.startFlow("dual-monitoring");
//        Thread.sleep(10000);
//
//        System.out.println("종료 요청");
//        flowEngine.shutdown();
//        workers.forEach(Thread::interrupt);
//    }

    // 9-6
    public static void main(String[] args) throws InterruptedException{
        FlowEngine flowEngine = new FlowEngine();

        Flow flow = new Flow("dual-monitoring")
                .addNode(new TimerNode("timer", 1000))

                .addNode(new TemperatureSensorNode("t-sensor", 15, 45))
                .addNode(new ThresholdFilterNode("t-filter", "temperature", 30.0))
                .addNode(new AlertNode("t-alert"))

                .addNode(new HumiditySensorNode("h-sensor", 30, 90))
                .addNode(new ThresholdFilterNode("h-filter", "humidity", 70.0))
                .addNode(new AlertNode("h-alert"))

                .addNode(new LogNode("log"))

                .addNode(new FileWriterNode("file-writer", "normal_temp_log.txt"))
                .connect("timer", "out", "t-sensor", "trigger")
                .connect("timer", "out", "h-sensor", "trigger")

                .connect("t-sensor", "out", "t-filter", "in")
                .connect("t-filter", "alert", "t-alert", "in")
                .connect("t-filter", "normal", "file-writer", "in")

                .connect("h-sensor", "out", "h-filter", "in")
                .connect("h-filter", "alert", "h-alert", "in")
                .connect("h-filter", "normal", "log", "in");

        flowEngine.register(flow);

        List<Thread> workers = new ArrayList<>();
        for (Flow f : flowEngine.getFlows().values()) {
            for (Flow.FlowConnection fc : f.getConnections()) {
                AbstractNode targetNode = f.getNodes().get(fc.getTargetNodeId());
                Thread thread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Message msg = fc.getConnection().poll();
                            if (msg != null && f.getState() == Flow.FlowState.RUNNING) {
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
            }
        }

        System.out.println("IoT 온도 모니터링 시스템 가동 시작");
        flowEngine.startFlow("dual-monitoring");
        Thread.sleep(10000);

        System.out.println("종료 요청");
        flowEngine.shutdown();
        workers.forEach(Thread::interrupt);
    }
}
