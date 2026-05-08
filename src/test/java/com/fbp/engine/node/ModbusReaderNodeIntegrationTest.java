package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModbusReaderNodeIntegrationTest {

    private ModbusTcpSimulator simulator;
    private ModbusReaderNode node;
    private static final int TEST_PORT = 5028;

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
                "startAddress", 0,
                "count", 1
        );
        node = new ModbusReaderNode("reader", config);
        node.initialize();

        assertTrue(node.isConnected());
    }

    @Test
    @DisplayName("레지스터 읽기 (trigger → 메시지 수신)")
    void testReadRegister() throws InterruptedException {
        simulator.setRegister(0, 1234);

        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        );
        node = new ModbusReaderNode("reader", config);
        node.initialize();

        Connection outConn = new LocalConnection();
        node.getOutputPort("out").connect(outConn);
        node.getInputPort("trigger").receive(new Message(Map.of()));

        Message resultMsg = outConn.poll();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getPayload());
    }

    @Test
    @DisplayName("registerMapping 적용")
    @SuppressWarnings("unchecked")
    void testRegisterMapping() throws InterruptedException {
        simulator.setRegister(0, 255); 
        simulator.setRegister(1, 600); 

        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "startAddress", 0,
                "count", 2,
                "registerMapping", Map.of(
                        "0", Map.of("name", "temperature", "scale", 0.1),
                        "1", Map.of("name", "humidity", "scale", 0.1)
                )
        );
        node = new ModbusReaderNode("reader", config);
        node.initialize();

        Connection outConn = new LocalConnection();
        node.getOutputPort("out").connect(outConn);

        node.getInputPort("trigger").receive(new Message(Map.of()));
        Message resultMsg = outConn.poll();

        assertNotNull(resultMsg);
        Map<String, Object> data = resultMsg.get("data");
        
        assertNotNull(data);
        assertEquals(25.5, ((Number) data.get("temperature")).doubleValue(), 0.01);
        assertEquals(60.0, ((Number) data.get("humidity")).doubleValue(), 0.01);
    }

    @Test
    @DisplayName("읽기 실패 시 에러 포트로 메시지 전달")
    void testErrorPortOnFailure() throws InterruptedException {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "startAddress", 100,
                "count", 1
        );
        node = new ModbusReaderNode("reader", config);
        node.initialize();

        Connection errConn = new LocalConnection();
        node.getOutputPort("error").connect(errConn);
        node.getInputPort("trigger").receive(new Message(Map.of()));

        Message errMsg = errConn.poll();
        assertNotNull(errMsg);
        assertTrue(errMsg.getPayload().containsKey("error"));
    }

    @Test
    @DisplayName("shutdown 후 연결 해제")
    void testDisconnectAfterShutdown() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", TEST_PORT,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        );
        node = new ModbusReaderNode("reader", config);
        node.initialize();
        assertTrue(node.isConnected());

        node.shutdown();
        assertFalse(node.isConnected());
    }
}