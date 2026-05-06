package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.JsonFlowParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FlowHandler implements HttpHandler {
    private final FlowManager flowManager;
    private final JsonFlowParser flowParser;

    public FlowHandler(FlowManager flowManager) {
        this.flowManager = flowManager;
        this.flowParser = new JsonFlowParser();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (method.equalsIgnoreCase("GET") && path.equals("/flows")) {
                handleGet(exchange);
            } else if (method.equals("POST") && path.equals("/flows")) {
                handlePost(exchange);
            } else if (method.equals("DELETE") && path.startsWith("/flows/")) {
                handleDelete(exchange, path);
            } else {
                ApiResponse.sendError(exchange, 405, "허용되지 않는 HTTP 메서드입니다.");
            }
        } catch (Exception e) {
            ApiResponse.sendError(exchange, 500, "서버 내부 오류: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> flowList = flowManager.getAllFlows().stream()
                .map(flow -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", flow.getId());
                    dto.put("name", flow.getName());
                    dto.put("status", flow.getState().name());
                    return dto;
                }).toList();
        ApiResponse.send(exchange, 200, flowList);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try (InputStream requestBody = exchange.getRequestBody()) {
            FlowDefinition definition = flowParser.parse(requestBody);
            flowManager.deploy(definition);

            Map<String, String> response = new HashMap<>();
            response.put("id", definition.getId());
            response.put("status", "DEPLOYED_AND_RUNNING");

            ApiResponse.send(exchange, 201, response);
        } catch (Exception e) {
            ApiResponse.sendError(exchange, 400, "플로우 배포 실패 (잘못된 JSON 형식): " + e.getMessage());
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length == 3) {
            String flowId = parts[2];
            try {
                flowManager.remove(flowId);
                Map<String, String> response = new HashMap<>();
                response.put("message", "플로우 [" + flowId + "] 가 성공적으로 삭제되었습니다.");
                ApiResponse.send(exchange, 200, response);
            } catch (IllegalArgumentException e) {
                ApiResponse.sendError(exchange, 404, "플로우를 찾을 수 없습니다: " + flowId);
            } catch (Exception e) {
                ApiResponse.sendError(exchange, 500, "플로우 삭제 중 오류 발생: " + e.getMessage());
            }
        } else {
            ApiResponse.sendError(exchange, 400, "잘못된 요청 경로입니다. 삭제할 플로우 ID가 필요합니다.");
        }
    }
}
