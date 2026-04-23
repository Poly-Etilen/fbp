package com.fbp.engine.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpClientIntegrationTest {

    private ModbusTcpSimulator simulator;
    private ModbusTcpClient client;
    private static final int TEST_PORT = 5028;

    @BeforeEach
    void setUp() throws IOException {
        simulator = new ModbusTcpSimulator(TEST_PORT, 10);
        simulator.start();
        client = new ModbusTcpClient("127.0.0.1", TEST_PORT);
    }

    @AfterEach
    void tearDown() {
        // 테스트가 끝날 때마다 자원 정리
        if (client != null) {
            client.disconnect();
        }
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("연결/해제")
    void testConnectAndDisconnect() throws IOException {
        assertFalse(client.isConnected());

        client.connect();
        assertTrue(client.isConnected());

        client.disconnect();
        assertFalse(client.isConnected());
    }

    @Test
    @DisplayName("Holding Register 읽기")
    void testReadHoldingRegister() throws Exception {
        simulator.setRegister(0, 5678);
        client.connect();
        int[] values = client.readHoldingRegisters(1, 0, 1);

        assertEquals(1, values.length);
        assertEquals(5678, values[0]);
    }

    @Test
    @DisplayName("다수 레지스터 읽기")
    void testReadMultipleRegisters() throws Exception {
        for (int i = 0; i < 5; i++) {
            simulator.setRegister(i, 100 + i); // 100, 101, 102, 103, 104
        }

        client.connect();
        int[] values = client.readHoldingRegisters(1, 0, 5);

        assertEquals(5, values.length);
        for (int i = 0; i < 5; i++) {
            assertEquals(100 + i, values[i]);
        }
    }

    @Test
    @DisplayName("Single Register 쓰기")
    void testWriteSingleRegister() throws Exception {
        client.connect();
        client.writeSingleRegister(1, 2, 9999);

        assertEquals(9999, simulator.getRegister(2));
    }

    @Test
    @DisplayName("쓰기 후 읽기")
    void testWriteAndRead() throws Exception {
        client.connect();
        client.writeSingleRegister(1, 3, 4321);
        int[] values = client.readHoldingRegisters(1, 3, 1);

        assertEquals(1, values.length);
        assertEquals(4321, values[0]);
    }

    @Test
    @DisplayName("에러 응답 처리 (ILLEGAL_DATA_ADDRESS)")
    void testErrorResponse() throws IOException {
        client.connect();

        ModbusException exception = assertThrows(ModbusException.class, () ->
            client.readHoldingRegisters(1, 100, 1));

        assertEquals(0x02, exception.getExceptionCode());
    }

    @Test
    @DisplayName("소켓 타임아웃 또는 연결 끊김")
    void testSocketTimeoutOrDisconnect() throws IOException, InterruptedException {
        client.connect();
        simulator.stop();
        Thread.sleep(100);

        assertThrows(IOException.class, () -> client.readHoldingRegisters(1, 0, 1));
    }
}