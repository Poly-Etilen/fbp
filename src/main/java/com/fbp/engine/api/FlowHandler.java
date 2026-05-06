package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.FlowDefinition;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class FlowHandler implements HttpHandler {
    private final FlowManager flowManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (method.equals("GET") && path.equals("/flows")) {
                handleList(exchange);
            } else if (method.equals("POST") && path.equals("/flows")) {
                handleDeploy(exchange);
            } else if (method.equals("DELETE") && path.startsWith("/flows/")) {
                hand
            }
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        List<String> flowIds = flowManager.getDeployedFlowIds();
        sendJsonResponse(exchange, 200, flowIds);
    }

    private void handleDeploy(HttpExchange exchange) throws IOException {
        FlowDefinition def = objectMapper.readValue(exchange.getRequestBody(), FlowDefinition.class);
        flowManager.deploy(def);
        sendJsonResponse(exchange, 201, "Deployed: " + def.getId());
    }

    private void handleRemove(HttpExchange exchange, String path) throws IOException {
        String flowId = path.substring("/flows/".length());
        if (flowId.isEmpty()) {
            sendResponse(exchange, 400, "Missing Flow ID");
            return;
        }
        flowManager.remove(flowId);
        sendResponse(exchange, 200, "Removed: " + flowId);
    }

    private void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int code, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
