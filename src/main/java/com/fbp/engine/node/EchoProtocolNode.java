package com.fbp.engine.node;

import com.fbp.engine.core.ConnectionState;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

@Slf4j
public class EchoProtocolNode extends ProtocolNode{

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public EchoProtocolNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void connect() throws Exception {
        String host = (String) config.getOrDefault("host", "localhost");
        int port = (int) config.getOrDefault("port", 9999);

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        log.info("[{}] Echo 서버({}:{}) 소켓 연결됨", getId(), host, port);
    }

    @Override
    protected void disconnect() {
        try {
            if (in != null) { in.close(); }
            if (out != null) { out.close(); }
            if (socket != null && !socket.isClosed()) { socket.close(); }
            log.info("[{}] Echo 서버 소켓 닫힘", getId());
        } catch (Exception e) {
            log.error("[{}] 소켓 닫기 중 에러 발생: {}", getId(), e.getMessage());
        }
    }

    @Override
    protected void onProcess(Message message) {
        if (!isConnected()) {
            log.warn("[{}] 연결 끊김 상태. 메시지 전송 무시: {}", getId(), message);
            return;
        }

        try {
            String sendData = message.toString();
            out.println(sendData);
            log.debug("[{}] 서버로 전송: {}", getId(), sendData);

            String response = in.readLine();
            if (response != null) {
                log.debug("[{}] 서버 응답 수신: {}", getId(), response);
                send("out", new Message(Map.of("echoData", response)));
            } else {
                throw new Exception("서버가 응답 없이 연결을 종료함.");
            }
        } catch (Exception e) {
            log.error("[{}] 통신 에러 발생: {}", getId(), e.getMessage());

            this.connectionState = ConnectionState.ERROR;
            disconnect();
            reconnect();
        }
    }
}
