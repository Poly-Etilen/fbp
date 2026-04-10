package com.fbp.engine.practice;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class EchoServer {
    public static void main(String[] args) {
        int port = 9999;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Echo 서버가 포트 " + port + "에서 대기 중");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("클라이언트 연결됨: {}", clientSocket.getInetAddress());

                try (
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
                        ) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        log.debug("수신: {}", inputLine);
                        out.println(inputLine);
                    }
                } catch (Exception e) {
                    log.warn("클라이언트 연결 종료 또는 에러: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("서버 구동 중 치명적 에러 발생", e);
        }
    }
}
