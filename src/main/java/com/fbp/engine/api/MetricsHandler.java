package com.fbp.engine.api;

import com.fbp.engine.metrics.MetricsCollector;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class MetricsHandler implements HttpHandler {
    private final MetricsCollector metricsCollector;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if (method.equalsIgnoreCase("GET")) {
                if (path.startsWith("/flows/") && path.endsWith("/metrics")) {
                    handleFlowMetrics(exchange, path);
                } else if (path.startsWith("/nodes") && path.endsWith("/stats")) {
                    handleNodeStats(exchange, path);
                } else {
                    ApiResponse.sendError(exchange, 404, "해당 메트릭 경로를 찾을 수 없음");
                }
            } else {
                ApiResponse.sendError(exchange, 405, "Method not Allowed");
            }
        } catch (Exception e) {
            ApiResponse.sendError(exchange, 500, "서버 내부 오류: " + e.getMessage());
        }
    }

    private void handleFlowMetrics(HttpExchange exchange, String path) throws IOException {
        // 경로: /flows/{id}/metrics
        String[] parts = path.split("/");
        if (parts.length == 4) {
            String flowId = parts[2];
            Object metrics = metricsCollector.getFlowMetrics(flowId);

            if (metrics != null) {
                ApiResponse.send(exchange, 200, metrics);
            } else {
                ApiResponse.sendError(exchange, 404, "지정된 플로우 메트릭을 찾을 수 없습니다: " + flowId);
            }
        } else {
            ApiResponse.sendError(exchange, 400, "잘못된 요청 경로입니다.");
        }
    }

    private void handleNodeStats(HttpExchange exchange, String path) throws IOException {
        // 경로: /nodes/{id}/stats
        String[] parts = path.split("/");
        if (parts.length == 4) {
            String nodeId = parts[2];
            Object stats = metricsCollector.getNodeMetrics(nodeId);

            if (stats != null) {
                ApiResponse.send(exchange, 200, stats);
            } else {
                ApiResponse.sendError(exchange, 404, "지정된 노드 통계를 찾을 수 없습니다: " + nodeId);
            }
        } else {
            ApiResponse.sendError(exchange, 400, "잘못된 요청 경로입니다.");
        }
    }
}
