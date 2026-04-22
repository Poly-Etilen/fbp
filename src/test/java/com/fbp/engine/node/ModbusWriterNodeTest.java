package com.fbp.engine.node;

import com.fbp.engine.core.ConnectionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModbusWriterNodeTest {

    @Test
    @DisplayName("포트 구성")
    void testPortConfiguration() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "alertCode"
        );
        ModbusWriterNode node = new ModbusWriterNode("writer", config);

        assertNotNull(node.getInputPort("in"));
        assertNotNull(node.getOutputPort("result"));
        assertNotNull(node.getOutputPort("error"));
    }

    @Test
    @DisplayName("초기 상태")
    void testInitialState() {
        Map<String, Object> config = Map.of(
                "host", "127.0.0.1",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "alertCode"
        );
        ModbusWriterNode node = new ModbusWriterNode("writer", config);

        assertFalse(node.isConnected());
        assertEquals(ConnectionState.DISCONNECTED, node.getConnectionState());
    }

    @Test
    @DisplayName("config 확인")
    void testConfigVerification() {
        Map<String, Object> config = Map.of(
                "host", "192.168.1.50",
                "port", 502,
                "slaveId", 2,
                "registerAddress", 100,
                "valueField", "controlValue",
                "scale", 10.0
        );
        ModbusWriterNode node = new ModbusWriterNode("writer", config);
        assertEquals("192.168.1.50", node.getConfig("host"));
        assertEquals(502, node.getConfig("port"));
        assertEquals(2, node.getConfig("slaveId"));
        assertEquals(100, node.getConfig("registerAddress"));
        assertEquals("controlValue", node.getConfig("valueField"));
        assertEquals(10.0, node.getConfig("scale"));
    }
}