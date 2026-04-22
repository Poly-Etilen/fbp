package com.fbp.engine.node;

import com.fbp.engine.core.ConnectionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModbusReaderNodeTest {

    @Test
    @DisplayName("포트 구성")
    void testPortConfiguration() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", 5020,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        );
        ModbusReaderNode node = new ModbusReaderNode("reader", config);

        assertNotNull(node.getInputPort("trigger"));
        assertNotNull(node.getOutputPort("out"));
        assertNotNull(node.getOutputPort("error"));
    }

    @Test
    @DisplayName("초기 상태")
    void testInitialState() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", 5020,
                "slaveId", 1,
                "startAddress", 0,
                "count", 1
        );
        ModbusReaderNode node = new ModbusReaderNode("reader", config);

        assertFalse(node.isConnected());
        assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
    }

    @Test
    @DisplayName("config 확인")
    void testConfigVerification() {
        Map<String, Object> config = Map.of(
                "host", "192.168.0.100",
                "port", 502,
                "slaveId", 1,
                "startAddress", 10,
                "count", 5
        );

        ModbusReaderNode node = new ModbusReaderNode("reader", config);
        assertEquals("192.168.0.100", node.getConfig("host"));
        assertEquals(502, node.getConfig("port"));
        assertEquals(1, node.getConfig("slaveId"));
        assertEquals(10, node.getConfig("startAddress"));
        assertEquals(5, node.getConfig("count"));
    }
}