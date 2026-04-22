package com.fbp.engine.node;

import com.fbp.engine.core.RuleExpression;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class CompositeRuleNode extends AbstractNode{
    public enum Operator {
        AND,
        OR
    }

    private final List<Predicate<Message>> conditions = new ArrayList<>();
    private final Operator operator;
    public CompositeRuleNode(String id, Operator operator) {
        super(id);
        this.operator = operator;

        addInputPort("in");
        addOutputPort("match");
        addInputPort("mismatch");
    }

    public CompositeRuleNode addCondition(Predicate<Message> condition) {
        this.conditions.add(condition);
        return this;
    }

    public CompositeRuleNode addCondition(String field, String op, Object value) {
        String expressionStr = field + " " + op + " " + value.toString();
        RuleExpression expression = RuleExpression.parse(expressionStr);

        this.conditions.add(expression::evaluate);
        return this;
    }

    @Override
    protected void onProcess(Message message) {
        if (message == null) {
            return;
        }

        try {
            boolean isMatch;
            if (operator == Operator.AND) {
                isMatch = conditions.stream().allMatch(cond -> cond.test(message));
            } else {
                isMatch = conditions.stream().anyMatch(cond -> cond.test(message));
            }

            if (isMatch) {
                log.debug("[{}] 복합 조건 만족 (연산자: {}) -> 'match' 포트로 전달", getId(), operator);
                send("match", message);
            } else {
                log.debug("[{}] 복합 조건 불만족 (연산자{}) -> 'mismatch' 포트로 전달", getId(), operator);
                send("mismatch", message);
            }
        } catch (Exception e) {
            log.warn("[{}] 복합 조건 평가 중 오류 발생: {}", getId(), e.getMessage());
            send("mismatch", message);
        }
    }
}
