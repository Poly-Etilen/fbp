package com.fbp.engine.core;


import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleExpressionTest {

    @Test
    @DisplayName("파싱 — 숫자 비교")
    void testNumericComparison() {
        RuleExpression expression = RuleExpression.parse("temperature > 30.0");

        Message matchMsg = new Message(Map.of("temperature", 35.0));
        Message mismatchMsg = new Message(Map.of("temperature", 25.0));

        assertTrue(expression.evaluate(matchMsg));
        assertFalse(expression.evaluate(mismatchMsg));
    }

    @Test
    @DisplayName("파싱 — 문자열 비교")
    void testStringComparison() {
        RuleExpression expression = RuleExpression.parse("status == ON");

        Message matchMsg = new Message(Map.of("status", "ON"));
        Message mismatchMsg = new Message(Map.of("status", "OFF"));

        assertTrue(expression.evaluate(matchMsg));
        assertFalse(expression.evaluate(mismatchMsg));
    }

    @Test
    @DisplayName("모든 연산자")
    void testAllOperators() {
        Message msg = new Message(Map.of("value", 50.0));

        assertTrue(RuleExpression.parse("value > 40.0").evaluate(msg));
        assertFalse(RuleExpression.parse("value > 60.0").evaluate(msg));

        assertTrue(RuleExpression.parse("value >= 50.0").evaluate(msg));
        assertTrue(RuleExpression.parse("value >= 40.0").evaluate(msg));
        assertFalse(RuleExpression.parse("value >= 60.0").evaluate(msg));

        assertTrue(RuleExpression.parse("value < 60.0").evaluate(msg));
        assertFalse(RuleExpression.parse("value < 40.0").evaluate(msg));

        assertTrue(RuleExpression.parse("value <= 50.0").evaluate(msg));
        assertTrue(RuleExpression.parse("value <= 60.0").evaluate(msg));
        assertFalse(RuleExpression.parse("value <= 40.0").evaluate(msg));

        assertTrue(RuleExpression.parse("value == 50.0").evaluate(msg));
        assertFalse(RuleExpression.parse("value == 40.0").evaluate(msg));

        assertTrue(RuleExpression.parse("value != 40.0").evaluate(msg));
        assertFalse(RuleExpression.parse("value != 50.0").evaluate(msg));
    }

    @Test
    @DisplayName("잘못된 표현식")
    void testInvalidExpression() {
        assertThrows(IllegalArgumentException.class, () -> RuleExpression.parse("invalid_expression"));
        RuleExpression invalidOpExpr = RuleExpression.parse("temperature => 30.0");
        Message dummyMsg = new Message(Map.of("temperature", 35.0));

        assertThrows(IllegalArgumentException.class, () -> invalidOpExpr.evaluate(dummyMsg));
    }

    @Test
    @DisplayName("필드 없음")
    void testMissingField() {
        RuleExpression expression = RuleExpression.parse("humidity > 50.0");
        Message msg = new Message(Map.of("temperature", 25.0));
        assertFalse(expression.evaluate(msg));
    }
}