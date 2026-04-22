package com.fbp.engine.protocol;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

// 과제 3-2
@Slf4j
@RequiredArgsConstructor
public class ModbusTcpClient {
    private final String host;
    private final int port;
    
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private int transactionId = 0;

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(3000);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("연결 해제 중 오류 발생", e);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private int getNextTransactionId() {
        transactionId = (transactionId + 1) % 65536;
        return transactionId;
    }

    // 읽기 요청 (FC 03: Read Holding Registers)
    public synchronized int[] readHoldingRegisters(int unitId, int startAddress, int quantity) throws IOException, ModbusException {
        int currentTxId = getNextTransactionId();

        out.write(buildMbapHeader(currentTxId, 6, unitId));
        out.writeByte(0x03);       // FC 03
        out.writeShort(startAddress);
        out.writeShort(quantity);
        out.flush();

        readMbapHeader(currentTxId);
        
        int fc = in.readUnsignedByte();

        if (fc == (0x03 | 0x80)) {
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(0x03, exceptionCode);
        }

        int byteCount = in.readUnsignedByte();
        int[] values = new int[byteCount / 2];

        for (int i = 0; i < values.length; i++) {
            values[i] = in.readUnsignedShort(); 
        }

        return values;
    }

    // 2. 쓰기 요청 (FC 06: Write Single Register)
    public synchronized void writeSingleRegister(int unitId, int address, int value) throws IOException, ModbusException {
        int currentTxId = getNextTransactionId();

        out.write(buildMbapHeader(currentTxId, 6, unitId));
        out.writeByte(0x06);
        out.writeShort(address);
        out.writeShort(value);
        out.flush();

        readMbapHeader(currentTxId);
        
        int fc = in.readUnsignedByte();

        if (fc == (0x06 | 0x80)) {
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(0x06, exceptionCode);
        }

        int respAddress = in.readUnsignedShort();
        int respValue = in.readUnsignedShort();

        if (respAddress != address || respValue != value) {
            throw new IOException("쓰기 응답 에코백(Echo-back) 불일치!");
        }
    }
    
    /**
     * MBAP 헤더 7바이트 생성
     */
    private byte[] buildMbapHeader(int txId, int length, int unitId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeShort(txId);    // Transaction ID (2바이트)
        dos.writeShort(0x0000);  // Protocol ID (항상 0)
        dos.writeShort(length);  // Length (2바이트)
        dos.writeByte(unitId);   // Unit ID (1바이트)
        
        return baos.toByteArray();
    }

    /**
     * 응답에서 MBAP 헤더 7바이트를 읽어 검증
     */
    private void readMbapHeader(int expectedTxId) throws IOException {
        int respTxId = in.readUnsignedShort();
        int respProtocol = in.readUnsignedShort();
        int respLength = in.readUnsignedShort();
        int respUnitId = in.readUnsignedByte();

        if (respTxId != expectedTxId) {
            throw new IOException("Transaction ID 불일치. 예상:" + expectedTxId + ", 실제:" + respTxId);
        }
    }
}