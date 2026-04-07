package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

// 과제 4-4
//@Slf4j
//public class App {
//    public static void main(String[] args) {
//        GeneratorNode generator = new GeneratorNode("sensor-A");
//        PrintNode printer = new PrintNode("printer-1");
//
//        Connection conn1 = new Connection();
//
//        generator.getOutputPort().connect(conn1);
//
//        log.info("--- 두 노드 스레드 분리 테스트 ---");
//        Thread producer = new Thread(() -> {
//            for (int i = 1; i <= 5; i++) {
//                try {
//                    Thread.sleep(1000);
//                    double temp = 20.0 + i;
//                    generator.generate("temperature", temp);
//                    log.info("[생산자] 메시지 생선 완료: {}", temp);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//            log.info("[생산자] 모든 데이터 생성 완료 및 스레드 종료");
//        }, "Producer-Thread");
//
//        Thread consumer = new Thread(() -> {
//            for (int i = 0; i < 5; i++) {
//                try {
//                    Message msg = conn1.poll();
//                    printer.process(msg);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//            log.info("[소비자] 모든 데이터 처리 완료 및 스레드 종료");
//        }, "Consumer-Thread");
//        producer.start();
//        consumer.start();
//    }
//}

// 과제 4-5
@Slf4j
public class App {
    private static volatile boolean running = true;
    public static void main(String[] args) throws InterruptedException{
        GeneratorNode generator = new GeneratorNode("sensor-A");
        FilterNode filter = new FilterNode("temp-filter", "temperature", 30.0);
        PrintNode printer = new PrintNode("printer-1");

        Connection conn1 = new Connection();
        Connection conn2 = new Connection();

        generator.getOutputPort().connect(conn1);
        filter.getOutputPort().connect(conn2);

        log.info("--- 3노드 스레드 파이프라인 ---");
        Thread thread1 = new Thread(() -> {
            int count = 0;
            while (running) {
                try {
                    double temp = 25.0 + (count % 10);
                    generator.generate("temperature", temp);
                    log.info("[Thread-1] 생산: 온도 {}", temp);

                    count++;
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("[Thread-1] 생산자 스레드 종료");
        }, "Thread-1(Generate)");
        Thread thread2 = new Thread(() -> {
            while (running) {
                try {
                    Message msg = conn1.poll();
                    filter.process(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("[Thread-2] 필터 스레드 종료");
        }, "Thread-2(Filter)");
        Thread thread3 = new Thread(() -> {
            while (running) {
                try {
                    Message msg = conn2.poll();
                    printer.process(msg);

                    log.info("[버퍼 상태] Conn1: {}, Conn2: {}", conn1.getBufferSize(), conn2.getBufferSize());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("[Thread-3] 출력 스레드 종료");
        }, "Thread-3(Print)");
        thread1.start();
        thread2.start();
        thread3.start();

        Thread.sleep(2000);
        log.info("--- 2초 경과, 중지 요청됨 ---");
        running = false;
        thread1.interrupt();
        thread2.interrupt();
        thread3.interrupt();
    }
}
