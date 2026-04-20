package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MqttSubscriberNodeUnitTest {

    private MqttSubscriberNode node;
    private Map<String, Object> config;

    @BeforeEach
    void setUp() {
        config = new HashMap<>();
        config.put("brokerUrl", "tcp://localhost:1883");
        config.put("clientId", "test-unit-sub");
        config.put("topic", "test/topic");

        node = new MqttSubscriberNode("test-sub-node", config);
    }

    @Test
    @DisplayName("포트 구성")
    void testOutputPortConfiguration() {
        assertNotNull(node.getOutputPort("out"), "out 출력 포트가 구성되어 있어야 합니다.");
    }

    @Test
    @DisplayName("초기 상태")
    void testInitialConnectionState() {
        assertFalse(node.isConnected(), "생성 직후에는 연결 상태가 false여야 합니다.");
    }

    @Test
    @DisplayName("config 조회")
    void testConfigRetrieval() throws Exception{
        Object brokerUrl = node.getConfig("brokerUrl");

        assertEquals("tcp://localhost:1883", brokerUrl, "config에 설정된 brokerUrl을 정상적으로 가져와야 합니다.");
    }

    @Test
    @DisplayName("JSON -> Message 변환")
    void testJsonParsingSuccess() throws Exception {
        Method parseMethod = MqttSubscriberNode.class.getDeclaredMethod("parsePayload", String.class);
        parseMethod.setAccessible(true);

        String validJson = "{\"temperature\": 25.5, \"humidity\": 60}";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(node, validJson);

        assertNotNull(result);
        assertEquals(25.5, result.get("temperature"));
        assertEquals(60, result.get("humidity"));
    }

    @Test
    @DisplayName("JSON 파싱 실패 처리")
    void testJsonParsingFailure() throws Exception {
        Method parseMethod = MqttSubscriberNode.class.getDeclaredMethod("parsePayload", String.class);
        parseMethod.setAccessible(true);

        String invalidJson = "Invalid JSON String!";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parseMethod.invoke(node, invalidJson);

        assertNotNull(result);
        assertEquals("Invalid JSON String!", result.get("rawPayload"));
    }
}