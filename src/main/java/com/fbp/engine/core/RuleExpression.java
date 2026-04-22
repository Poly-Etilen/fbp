package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RuleExpression {
    private final String field;
    private final String operator;
    private final Object value;

    public static RuleExpression parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("조건식이 비어 있음");
        }

        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("올바르지 않은 조건식 형식입니다. -> " + expression);
        }

        String field = parts[0];
        String operator = parts[1];
        Object value;

        try {
            value = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            value = parts[2];
        }
        return new RuleExpression(field, operator, value);
    }

    public boolean evaluate(Message message) {
        Object actual = message.getPayload().get(field);

        if (actual == null) {
            return false;
        }

        if (actual instanceof  Number && value instanceof Number) {
            double actualDouble = ((Number) actual).doubleValue();
            double targetDouble = ((Number) value).doubleValue();

            return switch (operator) {
                case ">" -> actualDouble > targetDouble;
                case ">=" -> actualDouble >= targetDouble;
                case "<" -> actualDouble < targetDouble;
                case "<=" -> actualDouble <= targetDouble;
                case "==" -> actualDouble == targetDouble;
                case "!=" -> actualDouble != targetDouble;
                default -> throw new IllegalArgumentException("지원하지 않는 숫자 연산자: " + operator);
            };
        } else {
            String actualStr = actual.toString();
            String targetStr = value.toString();

            return switch (operator) {
                case "==" -> actualStr.equals(targetStr);
                case "!=" -> !actualStr.equals(targetStr);
                case ">" -> actualStr.compareTo(targetStr) > 0;
                case ">=" -> actualStr.compareTo(targetStr) >= 0;
                case "<" -> actualStr.compareTo(targetStr) < 0;
                case "<=" -> actualStr.compareTo(targetStr) <= 0;
                default -> throw new IllegalArgumentException("지원하지 않는 문자열 연산자: " + operator);
            };
        }
    }
}
