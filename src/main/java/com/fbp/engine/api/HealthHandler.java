package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.engine.FlowManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;

@Slf4j
@RequiredArgsConstructor
public class HealthHandler implements HttpHandler {
    private final FlowManager flowManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime();
            int flowCount = flowManager.getDeployedFlowIds().size();

            HealthResponse responseData = new HealthResponse("UP", uptimeSeconds, flowCount);

            byte[] responseBytes = objectMapper.writeValueAsBytes(responseData);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
            log.debug("Health check 요청 처리 완료: status=UP, flowCount={}", flowCount);
        } catch (Exception e) {
            log.error("Health check 처리 중 오류 발생", e);
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
