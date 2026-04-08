package com.fbp.engine.runner;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.PrintNode;
import com.fbp.engine.node.TimerNode;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class App {
    public static void main(String[] args) throws InterruptedException{
        TimerNode timer = new TimerNode("timer-1", 500);
        FilterNode filter = new FilterNode("temp-filter", "tick", 3.0);
        PrintNode printer = new PrintNode("printer-1");

        Connection conn1 = new Connection();
        Connection conn2 = new Connection();

        timer.getOutputPort("out").connect(conn1);
        filter.getOutputPort("out").connect(conn2);

        log.info("--- TimerNode(0.5초 주기) → FilterNode(tick >= 3) → PrintNode 파이프라인 ---");
        Thread filterThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message msg = conn1.poll();
                    filter.process(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info("[Filter-Thread] 종료");
        }, "Filter-Thread");
        Thread printThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message msg = conn2.poll();
                    printer.process(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info("[Print-Thread] 종료");
        }, "Print-Thread");

        filterThread.start();
        printThread.start();

        log.info("시스템 초기화 및 가동");
        timer.initialize();
        filter.initialize();
        printer.initialize();

        Thread.sleep(3000);

        log.info("3초 경과, 시스템 종료 요청");
        timer.shutdown();
        filter.shutdown();
        printer.shutdown();

        filterThread.interrupt();
        printThread.interrupt();

        log.info("--- 파이프라인 종료 ---");
    }
}
