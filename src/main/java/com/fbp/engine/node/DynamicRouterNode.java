package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class DynamicRouterNode extends AbstractNode {
    private final List<RoutingRule> rules = new ArrayList<>();
    public static final String DEFAULT_PORT = "default";

    public DynamicRouterNode(String id) {
        super(id);
        addOutputPort(DEFAULT_PORT);
    }

    public void addRule(String portName, Predicate<Message> condition) {
        addOutputPort(portName);
        rules.add(new RoutingRule(portName, condition));
    }

    @Override
    protected void onProcess(Message message) {
        for (RoutingRule rule : rules) {
            if (rule.getCondition().test(message)) {
                log.debug("[{}] 메시지를 포트 '{}'로 라우팅합니다.", getId(), rule.getPortName());
                send(rule.getPortName(), message);
                return;
            }
        }
        log.debug("[{}] 매칭되는 규칙이 없어 기본 포트로 전송합니다.", getId());
        send(DEFAULT_PORT, message);
    }
}
