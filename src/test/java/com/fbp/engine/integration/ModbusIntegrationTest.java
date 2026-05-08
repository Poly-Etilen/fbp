package com.fbp.engine.integration;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.ModbusReaderNode;
import com.fbp.engine.node.ModbusWriterNode;
import com.fbp.engine.node.TransformNode;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ModbusIntegrationTest {

    private static final int PORT = 5020;
    private ModbusTcpSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new ModbusTcpSimulator(PORT, 10);
        simulator.start();
    }

    @AfterEach
    void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("Reader → 레지스터 읽기")
    void testReaderReadsRegisters() throws Exception {
        simulator.setRegister(0, 1234);

        ModbusReaderNode readerNode = new ModbusReaderNode("reader-node", Map.of(
                "host", "localhost",
                "port", PORT,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        ));

        Connection outConn = new LocalConnection();
        readerNode.getOutputPort("out").connect(outConn);
        readerNode.initialize();

        readerNode.process(new Message(Map.of()));

        assertEquals(1, outConn.getBufferSize());
        Message resultMsg = outConn.poll();
        int[] regs = (int[]) resultMsg.getPayload().get("registers");

        assertNotNull(regs);
        assertEquals(1234, regs[0]);

        readerNode.shutdown();
    }

    @Test
    @DisplayName("Writer → 레지스터 쓰기")
    void testWriterWritesRegisters() throws Exception {
        ModbusWriterNode writerNode = new ModbusWriterNode("writer-node", Map.of(
                "host", "localhost",
                "port", PORT,
                "slaveId", 1,
                "registerAddress", 5,
                "valueField", "targetValue",
                "scale", 1.0
        ));
        writerNode.initialize();

        Message inputMsg = new Message(Map.of("targetValue", 7777));
        writerNode.process(inputMsg);

        Thread.sleep(100);
        assertEquals(7777, simulator.getRegister(5));

        writerNode.shutdown();
    }

    @Test
    @DisplayName("Reader → Writer 파이프라인")
    void testReaderToWriterPipeline() throws Exception {
        simulator.setRegister(1, 400);
        ModbusReaderNode readerNode = new ModbusReaderNode("reader-node", Map.of(
                "host", "localhost",
                "port", PORT,
                "slaveId", 1,
                "startAddress", 1,
                "count", 1
        ));
        TransformNode transformNode = new TransformNode("transform-node", msg -> {
            int[] regs = (int[]) msg.getPayload().get("registers");
            Map<String, Object> newPayload = new HashMap<>();
            newPayload.put("writeVal", regs[0] + 100);
            return new Message(newPayload);
        });

        ModbusWriterNode writerNode = new ModbusWriterNode("writer-node", Map.of(
                "host", "localhost",
                "port", PORT,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "writeVal",
                "scale", 1.0
        ));

        Connection readerToTransform = new LocalConnection();
        Connection transformToWriter = new LocalConnection();

        readerNode.getOutputPort("out").connect(readerToTransform);
        transformNode.getOutputPort("out").connect(transformToWriter);

        readerNode.initialize();
        transformNode.initialize();
        writerNode.initialize();

        readerNode.process(new Message(Map.of()));
        if (readerToTransform.getBufferSize() > 0) {
            transformNode.process(readerToTransform.poll());
        }
        if (transformToWriter.getBufferSize() > 0) {
            writerNode.process(transformToWriter.poll());
        }
        Thread.sleep(100);
        assertEquals(500, simulator.getRegister(2));

        readerNode.shutdown();
        writerNode.shutdown();
    }

    @Test
    @DisplayName("연결 끊김 처리")
    void testConnectionErrorHandling() throws Exception {
        ModbusReaderNode readerNode = new ModbusReaderNode("reader-node", Map.of(
                "host", "localhost",
                "port", PORT,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        ));

        Connection errorConn = new LocalConnection();
        readerNode.getOutputPort("error").connect(errorConn);
        readerNode.initialize();

        simulator.stop();
        readerNode.process(new Message(Map.of()));

        assertEquals(1, errorConn.getBufferSize());
        Message errorMsg = errorConn.poll();
        assertNotNull(errorMsg.getPayload().get("error"));
        readerNode.shutdown();
    }
}