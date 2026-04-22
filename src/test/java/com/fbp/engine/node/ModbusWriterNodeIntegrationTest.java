package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModbusWriterNodeIntegrationTest {

    private ModbusTcpSimulator simulator;
    private ModbusWriterNode node;
    private static final int TEST_PORT = 5029; // 포트 충돌 방지

    @BeforeEach
    void setUp() throws InterruptedException {
        simulator = new ModbusTcpSimulator(TEST_PORT, 10);
        simulator.start();
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        if (node != null) {
            node.shutdown();
        }
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("연결 성공")
    void testConnectionSuccess() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "registerAddress", 0,
                "valueField", "val"
        );
        node = new ModbusWriterNode("writer", config);
        node.initialize();

        assertTrue(node.isConnected());
    }

    @Test
    @DisplayName("레지스터 쓰기")
    void testWriteRegister() throws InterruptedException {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "alertCode"
        );
        node = new ModbusWriterNode("writer", config);
        node.initialize();

        Message msg = new Message(Map.of("alertCode", 1));
        node.getInputPort("in").receive(msg);

        Thread.sleep(200);

        assertEquals(1, simulator.getRegister(2));
    }

    @Test
    @DisplayName("스케일 변환 (25.5 -> 255)")
    void testScaleConversion() throws InterruptedException {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "registerAddress", 3,
                "valueField", "temp",
                "scale", 10.0
        );
        node = new ModbusWriterNode("writer", config);
        node.initialize();
        Message msg = new Message(Map.of("temp", 25.5));
        node.getInputPort("in").receive(msg);

        Thread.sleep(200);
        assertEquals(255, simulator.getRegister(3));
    }

    @Test
    @DisplayName("shutdown 후 연결 해제")
    void testDisconnectAfterShutdown() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "registerAddress", 0,
                "valueField", "val"
        );
        node = new ModbusWriterNode("writer", config);
        node.initialize();
        assertTrue(node.isConnected());

        node.shutdown();
        assertFalse(node.isConnected());
    }
}