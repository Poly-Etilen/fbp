package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@Slf4j
public class App {
    public static void main(String[] args) throws InterruptedException{
        GeneratorNode generator = new GeneratorNode("generator-temp");
        TransformNode fToc = new TransformNode("f-to-c", message -> {
            double f = (double) message.get("F");
            double c = (f - 32) * 5 / 9;
            return new Message(Map.of("C", c));
        });
        PrintNode printer = new PrintNode("printer-1");

        Connection conn1 = new Connection();
        Connection conn2 = new Connection();

        generator.getOutputPort("out").connect(conn1);
        fToc.getOutputPort("out").connect(conn2);

        generator.generate("F", 100.0);
        fToc.process(conn1.poll());
        printer.process(conn2.poll());

        log.info("--- 분기 플로우 시작 ---");
        TimerNode timer = new TimerNode("timer", 1000);
        SplitNode split = new SplitNode("split", "tick", 3.0);
        PrintNode warnPrint = new PrintNode("print-warn");
        PrintNode normalPrint = new PrintNode("print-normal");

        Connection tToS = new Connection();
        Connection sToWarn = new Connection();
        Connection sToNormal = new Connection();

        timer.getOutputPort("out").connect(tToS);
        split.getOutputPort("match").connect(sToWarn);
        split.getOutputPort("mismatch").connect(sToNormal);

        Thread splitThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message msg = tToS.poll();
                    if (msg != null) {
                        split.process(msg);
                    } else  {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread warnThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message msg = sToWarn.poll();
                    if (msg != null) {
                        warnPrint.process(msg);
                    } else {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread normalThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message msg = sToNormal.poll();
                    if (msg != null) {
                        normalPrint.process(msg);
                    } else {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        splitThread.start();
        warnThread.start();
        normalThread.start();

        timer.initialize();
        Thread.sleep(7000);
        timer.shutdown();

        splitThread.interrupt();
        warnThread.interrupt();
        normalThread.interrupt();

    }
}
