package com.fbp.engine.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpSimulatorTest {

    private ModbusTcpSimulator simulator;
    private static final int TEST_PORT = 5027;

    @BeforeEach
    void setUp() throws InterruptedException{
        simulator = new ModbusTcpSimulator(TEST_PORT, 10);
        simulator.start();
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        if (simulator != null) {
            simulator.stop();
        }
    }

    @Test
    @DisplayName("시작/종료")
    void testStartAndStop() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", TEST_PORT)) {
            assertTrue(socket.isConnected());
        }

        simulator.stop();
        Thread.sleep(200);

        assertThrows(ConnectException.class, () -> new Socket("127.0.0.1", TEST_PORT));
    }

    @Test
    @DisplayName("레지스터 초기값")
    void testRegisterInitAndGet() {
        simulator.setRegister(5, 1234);

        assertEquals(1234, simulator.getRegister(5));
        assertEquals(-1, simulator.getRegister(-1));
        assertEquals(-1, simulator.getRegister(100));
    }

    @Test
    @DisplayName("FC 03 응답 (읽기)")
    void testFc03Response() throws Exception {
        simulator.setRegister(7, 7777);

        ModbusTcpClient client = new ModbusTcpClient("127.0.0.1", TEST_PORT);
        client.connect();

        int[] values = client.readHoldingRegisters(1, 7, 1);
        assertEquals(1, values.length);
        assertEquals(7777, values[0]);
        client.disconnect();
    }

    @Test
    @DisplayName("FC 06 응답 (쓰기)")
    void testFc06Response() throws Exception {
        ModbusTcpClient client = new ModbusTcpClient("127.0.0.1", TEST_PORT);
        client.connect();
        client.writeSingleRegister(1, 3, 5555);
        assertEquals(5555, simulator.getRegister(3));
        client.disconnect();
    }

    @Test
    @DisplayName("잘못된 주소 에러 (Exception Code 0x02)")
    void testInvalidAddressError() throws Exception {
        ModbusTcpClient client = new ModbusTcpClient("127.0.0.1", TEST_PORT);
        client.connect();
        ModbusException exception = assertThrows(ModbusException.class, () -> client.readHoldingRegisters(1, 50, 1));

        assertEquals(0x02, exception.getExceptionCode());
        client.disconnect();
    }

    @Test
    @DisplayName("다중 클라이언트 동시 접속")
    void testMultipleClients() throws Exception {
        ModbusTcpClient client1 = new ModbusTcpClient("127.0.0.1", TEST_PORT);
        ModbusTcpClient client2 = new ModbusTcpClient("127.0.0.1", TEST_PORT);

        client1.connect();
        client2.connect();
        client1.writeSingleRegister(1, 1, 1111);
        client2.writeSingleRegister(1, 2, 2222);

        assertEquals(1111, simulator.getRegister(1));
        assertEquals(2222, simulator.getRegister(2));

        int[] readByClient1 = client1.readHoldingRegisters(1, 2, 1);
        assertEquals(2222, readByClient1[0]);

        client1.disconnect();
        client2.disconnect();
    }
}