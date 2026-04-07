package com.fbp.engine.message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MessageTest {
    @Test
    @DisplayName("생성 시 ID 자동 할당")
    void test1() {
        Message msg = new Message(Map.of());
        Assertions.assertNotNull(msg.getId());
        Assertions.assertFalse(msg.getId().isEmpty());
    }

    @Test
    @DisplayName("생성 시 timestamp 자동 기록")
    void test2() {
        Message msg = new Message(Map.of());
        Assertions.assertTrue(msg.getTimestamp() > 0);
    }

    @Test
    @DisplayName("페이로드 조회")
    void test3() {
        Message msg = new Message(Map.of("key1", "value1"));
        Assertions.assertEquals("value1", msg.get("key1"));
    }

    @Test
    @DisplayName("제네릭 get 타입 캐스팅")
    void test4() {
        Message msg = new Message(Map.of("temperature", 25.5));
        Double temp = msg.get("temperature");
        Assertions.assertEquals(25.5, temp);
    }

    @Test
    @DisplayName("존재하지 않는 키 조회")
    void test5() {
        Message msg = new Message(Map.of("key1", "value1"));
        Assertions.assertNull(msg.get("없는키"));
    }

    @Test
    @DisplayName("페이로드 불변 - 외부 수정 차단")
    void test6() {
        Message msg = new Message(Map.of("key1", "value1"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> msg.getPayload().put("newKey", "newValue"));
    }

    @Test
    @DisplayName("페이로드 불변 - 원본 Map 수정 무영향")
    void test7() {
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("key1", "value1");

        Message msg = new Message(originalMap);
        originalMap.put("key1", "변조된값");

        Assertions.assertEquals("value1", msg.get("key1"));
    }

    @Test
    @DisplayName("withEntry - 새 객체 반환")
    void test8() {
        Message original = new Message(Map.of("key1", "value1"));
        Message modified = original.withEntry("key2", "value2");
        Assertions.assertNotSame(original, modified);
    }

    @Test
    @DisplayName("withEntry - 원본 불변")
    void test9() {
        Message original = new Message(Map.of("key1", "value1"));
        original.withEntry("key2", "value2");
        Assertions.assertFalse(original.hasKey("key2"));
    }

    @Test
    @DisplayName("withEntry - 새 메시지에 값 존재")
    void test10() {
        Message original = new Message(Map.of("key1", "value1"));
        Message modified = original.withEntry("key2", "value2");
        Assertions.assertEquals("value2", modified.get("key2"));
    }

    @Test
    @DisplayName("hasKey - 존재하는 키")
    void test11() {
        Message msg = new Message(Map.of("temperature", 25.5));
        Assertions.assertTrue(msg.hasKey("temperature"));
    }

    @Test
    @DisplayName("hasKey - 없는 키")
    void test12() {
        Message msg = new Message(Map.of("temperature", 25.5));
        Assertions.assertFalse(msg.hasKey("humidity"));
    }

    @Test
    @DisplayName("withoutKey - 키 제거 확인")
    void test13() {
        Message original = new Message(Map.of("key1", "value1", "key2", "value2"));
        Message modified = original.withoutKey("key1");
        Assertions.assertFalse(modified.hasKey("key1"));
        Assertions.assertTrue(modified.hasKey("key2"));
    }

    @Test
    @DisplayName("withoutKey - 원본 불변")
    void test14() {
        Message original = new Message(Map.of("key1", "value1"));
        original.withoutKey("key1");
        Assertions.assertTrue(original.hasKey("key1"));
    }

    @Test
    @DisplayName("toString 포맷")
    void test15() {
        Message msg = new Message(Map.of("key1", "value1"));
        String str = msg.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("payload="));
        Assertions.assertTrue(str.contains("key1=value1"));
    }
}
