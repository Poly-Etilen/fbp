package com.fbp.engine.protocol;

// 과제 3-3
public class ModbusException extends Exception {
    public static final int ILLEGAL_FUNCTION = 0x01;
    public static final int ILLEGAL_DATA_ADDRESS = 0x02;
    public static final int ILLEGAL_DATA_VALUE = 0x03;
    public static final int SLAVE_DEVICE_FAILURE = 0x04;

    private final int functionCode;
    private final int exceptionCode;

    public ModbusException(int functionCode, int exceptionCode) {
        super(String.format("MODBUS 에러 — FC: 0x%02X, Exception: 0x%02X (%s)", 
                functionCode, exceptionCode, getErrorMessage(exceptionCode)));
        this.functionCode = functionCode;
        this.exceptionCode = exceptionCode;
    }

    public int getExceptionCode() {
        return exceptionCode;
    }

    private static String getErrorMessage(int code) {
        return switch (code) {
            case ILLEGAL_FUNCTION -> "지원하지 않는 Function Code";
            case ILLEGAL_DATA_ADDRESS -> "존재하지 않는 레지스터 주소";
            case ILLEGAL_DATA_VALUE -> "값이 허용 범위를 벗어남";
            case SLAVE_DEVICE_FAILURE -> "장비 내부 오류";
            default -> "알 수 없는 오류";
        };
    }
}