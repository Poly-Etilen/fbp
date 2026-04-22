package com.fbp.engine.protocol;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

// 과제 3-4
@Slf4j
public class ModbusTcpSimulator {
    private final int port;
    private final int[] registers;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public ModbusTcpSimulator(int port, int registerCount) {
        this.port = port;
        this.registers = new int[registerCount];
    }

    public void setRegister(int address, int value) {
        if (address >= 0 && address < registers.length) {
            registers[address] = value;
        }
    }

    // 외부에서 레지스터 값 조회 (테스트 검증용)
    public int getRegister(int address) {
        if (address >= 0 && address < registers.length) {
            return registers[address];
        }
        return -1;
    }
    
    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log.info("MODBUS TCP 슬레이브 시작됨 (포트: {})", port);
                
                while (running) {
                    Socket clientSocket = serverSocket.accept(); // 클라이언트 접속 대기
                    log.info("마스터(클라이언트) 접속 됨: {}", clientSocket.getRemoteSocketAddress());
                    // 다중 클라이언트 동시 접속을 위한 스레드 분리
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                if (running) {
                    log.error("[Simulator] 서버 소켓 에러: {}", e.getMessage());
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log.info("MODBUS TCP 슬레이브 종료됨");
            }
        } catch (IOException e) {
            log.error("종료 중 에러: {}", e.getMessage());
        }
    }
    
    private void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (running && !socket.isClosed()) {
                // MBAP 헤더 수신 (7바이트)
                int txId = in.readUnsignedShort();
                int protocolId = in.readUnsignedShort();
                int length = in.readUnsignedShort();
                int unitId = in.readUnsignedByte();

                // 명령어(Function Code) 수신
                int fc = in.readUnsignedByte();

                // 명령어에 따른 분기 처리
                if (fc == 0x03) {
                    // FC 03 (읽기 요청)
                    int startAddress = in.readUnsignedShort();
                    int quantity = in.readUnsignedShort();

                    // 유효성 검사: 존재하지 않는 주소면 Exception Code 0x02 반환
                    if (startAddress < 0 || startAddress + quantity > registers.length) {
                        sendErrorResponse(out, txId, unitId, fc, 0x02); // ILLEGAL_DATA_ADDRESS
                    } else {
                        // 정상 응답 조립 (Unit ID(1) + FC(1) + ByteCount(1) + Data(N))
                        int byteCount = quantity * 2;
                        int respLength = 1 + 1 + 1 + byteCount; 

                        out.writeShort(txId);
                        out.writeShort(protocolId);
                        out.writeShort(respLength);
                        out.writeByte(unitId);
                        out.writeByte(fc);
                        out.writeByte(byteCount);

                        // 레지스터 값 데이터 연달아 기록
                        for (int i = 0; i < quantity; i++) {
                            out.writeShort(registers[startAddress + i]);
                        }
                        out.flush();
                    }

                } else if (fc == 0x06) {
                    // FC 06 (쓰기 요청)
                    int address = in.readUnsignedShort();
                    int value = in.readUnsignedShort();

                    // 유효성 검사: 범위를 벗어난 주소
                    if (address < 0 || address >= registers.length) {
                        sendErrorResponse(out, txId, unitId, fc, 0x02);
                    } else {
                        // 실제 레지스터에 값 기록
                        registers[address] = value;

                        // 정상 응답 조립
                        int respLength = 6; // Unit ID(1) + FC(1) + 주소(2) + 값(2)
                        out.writeShort(txId);
                        out.writeShort(protocolId);
                        out.writeShort(respLength);
                        out.writeByte(unitId);
                        out.writeByte(fc);
                        out.writeShort(address);
                        out.writeShort(value);
                        out.flush();
                    }

                } else {
                    // 구현하지 않은 명령어 처리 (Exception Code 0x01 반환)
                    sendErrorResponse(out, txId, unitId, fc, 0x01); // ILLEGAL_FUNCTION
                }
            }
        } catch (EOFException | SocketException e) {
            // 마스터가 정상적으로 연결을 끊었을 때 발생하는 예외
            log.info("마스터와 연결 종료됨");
        } catch (IOException e) {
            log.error("통신 에러: {}", e.getMessage());
        } finally {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (IOException e) {}
        }
    }
    
    private void sendErrorResponse(DataOutputStream out, int txId, int unitId, int fc, int exceptionCode) throws IOException {
        out.writeShort(txId);
        out.writeShort(0x0000);
        out.writeShort(3); // Length: Unit ID(1) + FC(1) + ExceptionCode(1) = 총 3바이트
        out.writeByte(unitId);
        out.writeByte(fc | 0x80); // 최상위 비트를 1로 설정하여 에러 알림!
        out.writeByte(exceptionCode);
        out.flush();
    }
}