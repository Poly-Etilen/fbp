package com.fbp.engine.exception;

import com.fbp.engine.protocol.ModbusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModbusExceptionTest {

    @Test
    @DisplayName("getMessage 포맷")
    void testGetMessageFormat() {
        int functionCode = 0x03;
        int exceptionCode = ModbusException.ILLEGAL_DATA_ADDRESS;

        ModbusException exception = new ModbusException(functionCode, exceptionCode);
        String message = exception.getMessage();

        String expectedMessage = "MODBUS 에러 — FC: 0x03, Exception: 0x02 (존재하지 않는 레지스터 주소)";
        assertEquals(expectedMessage, message);

        ModbusException unknownException = new ModbusException(0x06, 0xFF);
        assertEquals("MODBUS 에러 — FC: 0x06, Exception: 0xFF (알 수 없는 오류)", unknownException.getMessage());
    }

    @Test
    @DisplayName("getExceptionCode")
    void testGetExceptionCode() {
        int exceptionCode = ModbusException.ILLEGAL_DATA_VALUE;
        ModbusException exception = new ModbusException(0x06, exceptionCode);
        assertEquals(exceptionCode, exception.getExceptionCode());
    }

    @Test
    @DisplayName("상수 값")
    void testConstantValues() {
        assertEquals(0x01, ModbusException.ILLEGAL_FUNCTION);
        assertEquals(0x02, ModbusException.ILLEGAL_DATA_ADDRESS);
        assertEquals(0x03, ModbusException.ILLEGAL_DATA_VALUE);
        assertEquals(0x04, ModbusException.SLAVE_DEVICE_FAILURE);
    }
}