package com.fbp.engine.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModbusTcpClientTest {

    private DummyServer dummyServer;
    private ModbusTcpClient client;

    @BeforeEach
    void setUp() throws IOException {
        dummyServer = new DummyServer();
        dummyServer.start();
        client = new ModbusTcpClient("127.0.0.1", dummyServer.getPort());
    }

    @AfterEach
    void tearDown() {
        // 테스트 종료 후 자원 정리
        if (client != null) client.disconnect();
        if (dummyServer != null) dummyServer.stop();
    }

    @Test
    @DisplayName("FC 03 요청 프레임 조립")
    void testFc03Frame() throws Exception {
        client.connect();
        client.readHoldingRegisters(1, 10, 5);

        byte[] req = dummyServer.getLatestRequest();
        assertNotNull(req);
        assertEquals(12, req.length);

        assertEquals(1, req[6]);
        assertEquals(3, req[7]);
        assertEquals(0, req[8]);
        assertEquals(10, req[9]);
        assertEquals(0, req[10]);
        assertEquals(5, req[11]);
    }

    @Test
    @DisplayName("FC 06 요청 프레임 조립")
    void testFc06Frame() throws Exception {
        client.connect();
        client.writeSingleRegister(2, 20, 100);

        byte[] req = dummyServer.getLatestRequest();
        assertNotNull(req);
        assertEquals(12, req.length);

        // Unit ID (byte 6)
        assertEquals(2, req[6]);
        // Function Code (byte 7)
        assertEquals(6, req[7]);
        // Address (bytes 8, 9) = 20 (0x00, 0x14)
        assertEquals(0, req[8]);
        assertEquals(20, req[9]);
        // Value (bytes 10, 11) = 100 (0x00, 0x64)
        assertEquals(0, req[10]);
        assertEquals(100, req[11]);
    }

    @Test
    @DisplayName("3. MBAP 헤더 구조")
    void testMbapHeader() throws Exception {
        client.connect();
        client.readHoldingRegisters(5, 0, 1);

        byte[] req = dummyServer.getLatestRequest();
        assertNotNull(req);
        assertEquals(0, req[2]);
        assertEquals(0, req[3]);
        
        // Length는 이어지는 바이트 수(Unit ID 1바이트 + PDU)이므로 항상 6 (FC 03/06의 경우)
        assertEquals(0, req[4]);
        assertEquals(6, req[5]);
        
        // Unit ID (byte 6)
        assertEquals(5, req[6]);
        
        // Transaction ID는 1부터 시작 (bytes 0, 1)
        assertEquals(0, req[0]);
        assertEquals(1, req[1]);
    }

    @Test
    @DisplayName("Transaction ID 증가")
    void testTransactionIdIncrement() throws Exception {
        client.connect();

        client.readHoldingRegisters(1, 0, 1);
        byte[] req1 = dummyServer.getRequests().get(0);
        int txId1 = (req1[0] << 8) | (req1[1] & 0xFF);

        client.writeSingleRegister(1, 0, 10);
        byte[] req2 = dummyServer.getRequests().get(1);
        int txId2 = (req2[0] << 8) | (req2[1] & 0xFF);
        
        assertEquals(1, txId1);
        assertEquals(2, txId2);
    }

    @Test
    @DisplayName("초기 상태")
    void testInitialState() {
        ModbusTcpClient newClient = new ModbusTcpClient("127.0.0.1", 5020);
        assertFalse(newClient.isConnected());
    }

    private static class DummyServer {
        private ServerSocket serverSocket;
        private Thread serverThread;
        private final List<byte[]> requests = new ArrayList<>();
        private volatile boolean running = false;

        public void start() throws IOException {
            serverSocket = new ServerSocket(0);
            running = true;
            serverThread = new Thread(() -> {
                try (Socket socket = serverSocket.accept();
                     // 일반 InputStream 대신 DataInputStream 사용
                     java.io.DataInputStream dis = new java.io.DataInputStream(socket.getInputStream());
                     OutputStream os = socket.getOutputStream()) {

                    while (running && !socket.isClosed()) {
                        byte[] buffer = new byte[12];
                        try {
                            // [핵심 변경 포인트] 12바이트가 모두 도착할 때까지 안전하게 대기
                            dis.readFully(buffer);
                        } catch (java.io.EOFException e) {
                            break; // 클라이언트가 소켓 연결을 종료하면 루프 탈출
                        }

                        // 요청 바이트 복사 및 저장
                        byte[] req = new byte[12];
                        System.arraycopy(buffer, 0, req, 0, 12);
                        synchronized (requests) {
                            requests.add(req);
                        }

                        // 가짜 정상 응답 조립 및 전송
                        int fc = buffer[7];
                        os.write(buffer[0]);
                        os.write(buffer[1]);
                        os.write(0); os.write(0);

                        if (fc == 3) {
                            os.write(0); os.write(5);
                            os.write(buffer[6]);
                            os.write(3);
                            os.write(2);
                            os.write(0); os.write(0);
                        } else if (fc == 6) {
                            os.write(0); os.write(6);
                            os.write(buffer[6]);
                            os.write(6);
                            os.write(buffer, 8, 4); // Address, Value를 그대로 에코백
                        }
                        os.flush();
                    }
                } catch (IOException e) {
                    // 소켓 종료 등에 의한 예외는 무시
                }
            });
            serverThread.start();
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public byte[] getLatestRequest() {
            synchronized (requests) {
                if (requests.isEmpty()) return null;
                return requests.get(requests.size() - 1);
            }
        }

        public List<byte[]> getRequests() {
            synchronized (requests) {
                return new ArrayList<>(requests);
            }
        }

        public void stop() {
            running = false;
            try {
                if (serverSocket != null) serverSocket.close();
                if (serverThread != null) serverThread.join(1000);
            } catch (Exception e) {
                // 종료 시 발생하는 예외 무시
            }
        }
    }
}