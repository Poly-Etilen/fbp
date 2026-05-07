package com.fbp.engine.metrics;

import com.fbp.engine.api.HttpApiServer;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.FlowDefinition;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class HttpApiServerTest {
    private static HttpApiServer apiServer;
    private static FlowManager mockFlowManager;
    private static MetricsCollector mockMetricsCollector;
    private static HttpClient httpClient;
    private static final int TEST_PORT = 8081;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    @BeforeAll
    static void setup() throws IOException {
        mockFlowManager = mock(FlowManager.class);
        mockMetricsCollector = mock(MetricsCollector.class);
        apiServer = new HttpApiServer(TEST_PORT, mockFlowManager, mockMetricsCollector);
        apiServer.start();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (apiServer != null) {
            apiServer.stop();
        }
    }

    @Test
    @DisplayName("서버 시작/정지")
    void testServerLifecycle() throws IOException {
        int tempPort = 8082;
        HttpApiServer tempServer = new HttpApiServer(tempPort, mockFlowManager, mockMetricsCollector);
        assertDoesNotThrow(tempServer::start);

        try {
            java.net.Socket socket = new java.net.Socket("localhost", tempPort);
            assertTrue(socket.isConnected());
            socket.close();
        } catch (IOException e) {
            fail("서버가 시작되었으나 포트에 연결할 수 없습니다.");
        }

        assertDoesNotThrow(tempServer::stop);
    }

    @Test
    @DisplayName("GET /health")
    void testHealthCheck() throws IOException, InterruptedException {
        when(mockFlowManager.getDeployedFlowIds()).thenReturn(Collections.emptyList());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @DisplayName("GET /flows")
    void testGetFlows() throws IOException, InterruptedException {
        when(mockFlowManager.getAllFlows()).thenReturn(List.of());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("POST /flows")
    void testPostFlowSuccess() throws IOException, InterruptedException {
        String validJson = """
            {
              "id": "new-test-flow",
              "name": "테스트용 플로우",
              "nodes": [
                {
                  "id": "sensor",
                  "type": "TimerNode",
                  "config": { "period": 1000 }
                }
              ],
              "connections": []
            }
            """;
        doNothing().when(mockFlowManager).deploy(any());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(validJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"id\":\"new-test-flow\""));
        assertTrue(response.body().contains("DEPLOYED_AND_RUNNING"));
    }

    @Test
    @DisplayName("POST /flows 잘못된 JSON")
    void testPostFlowInvalidJson() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{ invalid json }"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("error"));
    }

    @Test
    @DisplayName("DELETE /flows/{id}")
    void testDeleteFlowSuccess() throws IOException, InterruptedException {
        String flowId = "active-flow";
        doNothing().when(mockFlowManager).remove(flowId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows/" + flowId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("성공적으로 삭제되었습니다"));
        verify(mockFlowManager, times(1)).remove(flowId);
    }

    @Test
    @DisplayName("DELETE /flows/{id} 없는 id")
    void testDeleteFlowNotFound() throws IOException, InterruptedException {
        doThrow(new IllegalArgumentException("Not found")).when(mockFlowManager).remove("non-existent");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows/non-existent"))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("GET /flows/{id}/metrics")
    void testGetFlowMetrics() throws IOException, InterruptedException {
        String flowId = "test-flow";
        FlowDefinition mockDef = mock(FlowDefinition.class);
        when(mockFlowManager.getDefinition(flowId)).thenReturn(mockDef);
        when(mockMetricsCollector.createFlowMetrics(mockDef)).thenReturn(null);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/flows/" + flowId + "/metrics"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("존재하지 않는 경로")
    void testNotFoundPath() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/invalid/path"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("잘못된 HTTP 메서드")
    void testWrongMethod() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    @DisplayName("포트 충돌")
    void testPortConflict() {
        assertThrows(IOException.class, () -> new HttpApiServer(TEST_PORT, mockFlowManager, mockMetricsCollector));
    }

    @Test
    @DisplayName("Content-Type")
    void testContentTypeHeader() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.contains("application/json"));
    }
}