package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class RoutingRule {
    private final String portName;
    private final Predicate<Message> condition;

    public boolean matches(Message message) {
        return condition.test(message);
    }
}
