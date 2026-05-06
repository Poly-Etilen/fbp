package com.fbp.engine.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonFlowParserTest {
    private JsonFlowParser parser;

    private final String VALID_JSON = """
            {
              "id": "test-flow",
              "name": "테스트 플로우",
              "nodes": [
                {
                  "id": "sensor",
                  "type": "TemperatureSensor",
                  "config": { "threshold": 30.0 }
                },
                {
                  "id": "rule",
                  "type": "ThresholdFilter",
                  "config": {}
                }
              ],
              "connections": [
                { "from": "sensor:out", "to": "rule:in" }
              ]
            }
            """;

    @BeforeEach
    public void setUp() {
        parser = new JsonFlowParser();
    }

    private InputStream toJsonStream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("정상 파싱")
    void testParseValidJson() {
        FlowDefinition def = parser.parse(toJsonStream(VALID_JSON));
        assertNotNull(def);
        assertEquals("test-flow", def.getId());
        assertEquals("테스트 플로우", def.getName());
    }

    @Test
    @DisplayName("노드 목록")
    void testNodeListParsing() {
        FlowDefinition def = parser.parse(toJsonStream(VALID_JSON));
        assertNotNull(def.getNodes());
        assertEquals(2, def.getNodes().size());

        NodeDefinition node1 = def.getNodes().get(0);
        assertEquals("sensor", node1.getId());
        assertEquals("TemperatureSensor", node1.getType());
        assertEquals(30.0, node1.getConfig().get("threshold"));
    }

    @Test
    @DisplayName("연결 목록")
    void testConnectionListParsing() {
        FlowDefinition def = parser.parse(toJsonStream(VALID_JSON));
        assertNotNull(def.getConnections());
        assertEquals(1, def.getConnections().size());
        ConnectionDefinition connection = def.getConnections().get(0);
        assertEquals("sensor:out", connection.getFrom());
        assertEquals("rule:in", connection.getTo());
    }

    @Test
    @DisplayName("필수 필드 누락 - id")
    void testMissingFlowId() {
        String json = """
                {
                    "nodes": [ { "id": "node1", "type" : "TestType" } ]
                }
                """;

        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("필수 필드 누락 - nodes")
    void testMissingNodesArray() {
        String json = """
                {
                  "id": "flow-1",
                  "name": "No Nodes Flow"
                }
                """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("빈 노드 목록")
    void testEmptyNodesList() {
        String json = """
                {
                    "id": "flow-1",
                    "nodes": []
                }
            """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("잘못된 JSON 형식")
    void testInvalidJsonSyntax() {
        String invalidJson = "{ id: missing-quotes, nodes: [ ] }";
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(invalidJson)));
    }

    @Test
    @DisplayName("연결의 포트 파싱")
    void testConnectionPortParsing() {
        String json = """
                {
                  "id": "flow-8",
                  "nodes": [
                    { "id": "sensor", "type": "Mock" },
                    { "id": "rule", "type": "Mock" }
                  ],
                  "connections": [
                    { "from": "sensor:out", "to": "rule:in" }
                  ]
                }
                """;
        FlowDefinition def = parser.parse(toJsonStream(json));
        ConnectionDefinition conn = def.getConnections().get(0);
        assertEquals("sensor:out", conn.getFrom());
        assertTrue(conn.getFrom().contains(":"));
    }

    @Test
    @DisplayName("잘못된 연결 형식")
    void testInvalidConnectionFormat() {
        String json = """
                {
                  "id": "flow-9",
                  "nodes": [ { "id": "sensor", "type": "Mock" } ],
                  "connections": [
                    { "from": "sensor", "to": "rule:in" }
                  ]
                }
                """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("존재하지 않는 노드 참조")
    void testReferenceUnknownNode() {
        String json = """
                {
                  "id": "flow-10",
                  "nodes": [ { "id": "sensor", "type": "Mock" } ],
                  "connections": [
                    { "from": "sensor:out", "to": "unknown-rule:in" }
                  ]
                }
                """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("중복 노드 id")
    void testDuplicateNodeId() {
        String json = """
                {
                  "id": "flow-11",
                  "nodes": [
                    { "id": "duplicate-node", "type": "TypeA" },
                    { "id": "duplicate-node", "type": "TypeB" }
                  ]
                }
                """;
        assertThrows(FlowParserException.class, () -> parser.parse(toJsonStream(json)));
    }

    @Test
    @DisplayName("config 타입 보존")
    void testConfigTypePreservation() {
        String json = """
                {
                  "id": "type-test",
                  "nodes": [
                    {
                      "id": "config-node",
                      "type": "ConfigNode",
                      "config": {
                        "stringValue": "hello",
                        "numberValue": 42.5,
                        "booleanValue": true
                      }
                    }
                  ]
                }
                """;

        FlowDefinition def = parser.parse(toJsonStream(json));
        Map<String, Object> config = def.getNodes().get(0).getConfig();

        assertTrue(config.get("stringValue") instanceof String);
        assertEquals("hello", config.get("stringValue"));
        assertTrue(config.get("numberValue") instanceof Double);
        assertEquals(42.5, config.get("numberValue"));
        assertTrue(config.get("booleanValue") instanceof Boolean);
        assertEquals(true, config.get("booleanValue"));
    }
}
