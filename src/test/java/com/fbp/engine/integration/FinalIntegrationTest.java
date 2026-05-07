package com.fbp.engine.integration;

import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class FinalIntegrationTest {
    private final String TEST_FILE = "test_final_normal_temp.txt";
    private FlowEngine engine;
    private CollectorNode alertCollector;
    private CollectorNode normalCollector;
    private CollectorNode totalCollector;

    @BeforeEach
    void setUpAndRun() throws InterruptedException, IOException {
        Files.deleteIfExists(Paths.get(TEST_FILE));
        engine = new FlowEngine();
        alertCollector = new CollectorNode("alert-collector");
        normalCollector = new CollectorNode("normal-collector");
        totalCollector = new CollectorNode("total-collector");

        Flow flow = new Flow("final-test", "최종 테스트")
                .addNode(new TimerNode("timer", 10))
                .addNode(new TemperatureSensorNode("sensor", 15.0, 45.0))
                .addNode(new ThresholdFilterNode("filter", "temperature", 30.0))
                .addNode(new FileWriterNode("file-writer", TEST_FILE))
                .addNode(alertCollector)
                .addNode(normalCollector)
                .addNode(totalCollector)

                .connect("timer", "out", "sensor", "trigger")

                .connect("sensor", "out", "filter", "in")
                .connect("sensor", "out", "total-collector", "in")

                .connect("filter", "alert", "alert-collector", "in")
                .connect("filter", "normal", "normal-collector", "in")
                .connect("filter", "normal", "file-writer", "in");

        engine.register(flow);

        List<Thread> workers = new ArrayList<>();
        for (Flow.FlowConnection fc : flow.getConnections()) {
            AbstractNode targetNode = flow.getNodes().get(fc.getTargetNodeId());
            Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Message msg = fc.getConnection().poll();
                        if (msg != null && flow.getState()== Flow.FlowState.RUNNING) {
                            targetNode.process(msg);
                        } else {
                            Thread.sleep(2);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            workers.add(thread);
            thread.start();
        }

        engine.startFlow("final-test");
        Thread.sleep(1000);
        Thread.sleep(100);
        engine.shutdown();
        workers.forEach(Thread::interrupt);
    }

    @AfterEach
    void cleanUp() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_FILE));
    }

    @Test
    @DisplayName("엔진 시작/종료")
    void test1() {
        Flow flow = engine.getFlows().get("final-test");
        Assertions.assertEquals(Flow.FlowState.STOPPED, flow.getState());
    }

    @Test
    @DisplayName("alert 경로 정확성")
    void test2() {
        Assertions.assertFalse(alertCollector.getCollected().isEmpty());
        for (Message msg : alertCollector.getCollected()) {
            Assertions.assertTrue((double) msg.get("temperature") > 30.0);
        }
    }

    @Test
    @DisplayName("normal 경로 정확성")
    void test3() {
        Assertions.assertFalse(normalCollector.getCollected().isEmpty());
        for (Message msg : normalCollector.getCollected()) {
            Assertions.assertTrue((double) msg.get("temperature") <= 30.0);
        }
    }

    @Test
    @DisplayName("전체 분기 완전성")
    void test4() {
        int totalGenerated = totalCollector.getCollected().size();
        int totalProcessed = alertCollector.getCollected().size() + normalCollector.getCollected().size();

        Assertions.assertTrue(totalGenerated > 0);
        Assertions.assertEquals(totalGenerated, totalProcessed);
    }

    @Test
    @DisplayName("파일 기록 검증")
    void test5() throws IOException{
        List<String> lines = Files.readAllLines(Paths.get(TEST_FILE));
        int normalCount = normalCollector.getCollected().size();

        Assertions.assertEquals(normalCount, lines.size());
    }

    @Test
    @DisplayName("센서 데이터 형식")
    void test6() throws IOException{
        for (Message msg : totalCollector.getCollected()) {
            Assertions.assertNotNull(msg.get("sensorId"));
            Assertions.assertNotNull(msg.get("temperature"));
            Assertions.assertNotNull(msg.get("unit"));
        }
    }

    @Test
    @DisplayName("온도 범위")
    void test7() {
        for (Message msg : totalCollector.getCollected()) {
            double temp = (double) msg.get("temperature");
            Assertions.assertTrue(temp >= 15.0 && temp <= 45.0);
        }
    }
}
