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
    // 8-2
//    public static void main(String[] args) throws InterruptedException{
//        Flow flow = new Flow("monitoring");
//        flow.addNode(new TimerNode("timer", 1000))
//                .addNode(new SplitNode("split", "tick", 3.0))
//                .addNode(new PrintNode("print-warn"))
//                .addNode(new PrintNode("print-normal"))
//                .connect("timer", "out", "split", "in")
//                .connect("split", "match", "print-warn", "in")
//                .connect("split", "mismatch", "print-normal", "in");
//
//        FlowEngine engine = new FlowEngine();
//        engine.register(flow);
//
//        List<Thread> workers = new ArrayList<>();
//
//        for (Flow.FlowConnection fc : flow.getConnections()) {
//            AbstractNode targetNode = flow.getNodes().get(fc.getTargetNodeId());
//            Thread thread = new Thread(() -> {
//                while (!Thread.currentThread().isInterrupted()) {
//                    try {
//                        Message msg = fc.getConnection().poll();
//                        if (msg != null) {
//                            targetNode.process(msg);
//                        } else {
//                            Thread.sleep(10);
//                        }
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }, "worker-" + fc.getId());
//
//            workers.add(thread);
//            thread.start();
//        }
//
//        engine.startFlow("monitoring");
//        Thread.sleep(5000);
//        engine.shutdown();
//        workers.forEach(Thread::interrupt);
//    }

    // 8-3
//    public static void main(String[] args) throws InterruptedException{
//        FlowEngine flowEngine = new FlowEngine();
//        Flow flowA = new Flow("Flow-A")
//                .addNode(new TimerNode("timer-A", 500))
//                .addNode(new PrintNode("print-A"))
//                .connect("timer-A","out","print-A","in");
//
//        Flow flowB = new Flow("Flow-B")
//                .addNode(new TimerNode("timer-B", 1000))
//                .addNode(new PrintNode("print-B"))
//                .connect("timer-B","out","print-B","in");
//
//        flowEngine.register(flowA);
//        flowEngine.register(flowB);
//
//        List<Thread> workers = new ArrayList<>();
//        for (Flow registeredFlow : flowEngine.getFlows().values()) {
//            for (Flow.FlowConnection fc : registeredFlow.getConnections()) {
//                AbstractNode targetNode = registeredFlow.getNodes().get(fc.getTargetNodeId());
//                Thread thread = new Thread(() -> {
//                    while (!Thread.currentThread().isInterrupted()) {
//                        try {
//                            Message msg = fc.getConnection().poll();
//                            if (msg != null) {
//                                targetNode.process(msg);
//                            } else {
//                                Thread.sleep(10);
//                            }
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                        }
//                    }
//                }, "worker-" + fc.getId());
//
//                workers.add(thread);
//                thread.start();
//            }
//        }
//
//        log.info("Starting workers...");
//        flowEngine.startFlow("Flow-A");
//        flowEngine.startFlow("Flow-B");
//
//        Thread.sleep(5000);
//
//        log.info("Shutdown workers...");
//        flowEngine.shutdown();
//
//        workers.forEach(Thread::interrupt);
//    }

    // 8-4
//    public static void main(String[] args) throws InterruptedException{
//        FlowEngine flowEngine = new FlowEngine();
//        flowEngine.register(new Flow("Flow-A").addNode(new TimerNode("tA", 1000))
//                        .addNode(new PrintNode("pA"))
//                .connect("tA", "out", "pA", "in"));
//
//        flowEngine.register(new Flow("Flow-B")
//                .addNode(new TimerNode("tB", 2000))
//                .addNode(new PrintNode("pB"))
//                .connect("tB","out","pB","in"));
//
//        List<Thread> workers = new ArrayList<>();
//        for (Flow flow : flowEngine.getFlows().values()) {
//            for (Flow.FlowConnection fc : flow.getConnections()) {
//                AbstractNode targetNode = flow.getNodes().get(fc.getTargetNodeId());
//                Thread thread = new Thread(() -> {
//                    while (!Thread.currentThread().isInterrupted()) {
//                        try {
//                            Message msg = fc.getConnection().poll();
//                            if (msg != null && flow.getState() == Flow.FlowState.RUNNING) {
//                                targetNode.process(msg);
//                            } else {
//                                Thread.sleep(10);
//                            }
//                        } catch (InterruptedException e) {
//                            Thread.currentThread().interrupt();
//                        }
//                    }
//                }, "worker-" + fc.getId());
//
//                workers.add(thread);
//                thread.start();
//            }
//        }
//
//        log.info("엔진 구동 전 상태 확인");
//        flowEngine.listFlows();
//
//        log.info("A만 시작");
//        flowEngine.startFlow("Flow-A");
//        flowEngine.listFlows();
//
//        Thread.sleep(3500);
//
//        log.info("B 시작");
//        flowEngine.startFlow("Flow-B");
//        flowEngine.listFlows();
//
//        Thread.sleep(4500);
//
//        log.info("Shutdown workers...");
//        flowEngine.shutdown();
//        workers.forEach(Thread::interrupt);
//    }

    // 8-5
    public static void main(String[] args){
        FlowEngine flowEngine = new FlowEngine();

        flowEngine.register(new Flow("monitoring")
                .addNode(new TimerNode("tA", 1000))
                .addNode(new PrintNode("pA"))
                .connect("tA", "out", "pA", "in"));

        List<Thread> workers = new ArrayList<>();
        for (Flow flow : flowEngine.getFlows().values()) {
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
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("명령어: list, start <id>, stop <id>, exit");

        while (true) {
            System.out.print("fbp> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {continue;}

            String[] parts = input.split("\\s+");
            String cmd = parts[0].toLowerCase();

            try {
                if (cmd.equals("list")) {
                    int index = 1;
                    for (Flow f : flowEngine.getFlows().values()) {
                        System.out.printf("[%d] %s %s%n", index++, f.getId(), f.getState());
                    }
                } else if (cmd.equals("start")) {
                    if (parts.length < 2) {
                        System.out.println("사용법: start <id>");
                    } else {
                        flowEngine.startFlow(parts[1]);
                    }
                } else if (cmd.equals("stop")) {
                    if (parts.length < 2) {
                        System.out.println("사용법: stop <id>");
                    } else {
                        flowEngine.stopFlow(parts[1]);
                    }
                } else if (cmd.equals("exit")) {
                    flowEngine.shutdown();
                    System.out.println("[Engine] 엔진 종료됨");
                    break;
                } else {
                    System.out.println("알 수 없는 명령어");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("에러: " + e.getMessage());
            }
        }

        workers.forEach(Thread::interrupt);
        scanner.close();
    }
}
