package com.fbp.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ApiResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void send(HttpExchange exchange, int code, Object data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        String responseBody = data != null ? mapper.writeValueAsString(data) : "{}";
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        send(exchange, statusCode, new ErrorResponse(message));
    }

    private record ErrorResponse(String error) {}
}
