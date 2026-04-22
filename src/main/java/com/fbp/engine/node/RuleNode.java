package com.fbp.engine.node;

import com.fbp.engine.core.RuleExpression;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
public class RuleNode extends AbstractNode{
    private final Predicate<Message> condition;
    public RuleNode(String id, Predicate<Message> condition) {
        super(id);
        this.condition = condition;

        addInputPort("in");
        addOutputPort("match");
        addOutputPort("mismatch");
    }

    public RuleNode(String id, String expressionStr) {
        this(id, RuleExpression.parse(expressionStr) :: evaluate);
    }

    @Override
    protected void onProcess(Message message) {
        if (message == null) {
            return;
        }

        try {
            if (condition.test(message)) {
                log.debug("[{}] 메시지 조건 만족 -> 'match' 포트로 전달", getId());
                send("match", message);
            } else  {
                log.debug("[{}] 메시지 조건 불만족 -> 'mismatch' 포트로 전달", getId());
                send("mismatch", message);
            }
        } catch (Exception e) {
            log.warn("[{}] 조건 평가 중 오류 발생 (핑드 누락 등): {}", getId(), e.getMessage());
            send("mismatch", message);
        }
    }
}
