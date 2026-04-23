package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

@Slf4j
public class TimeWindowRuleNode extends AbstractNode{
    private final Predicate<Message> condition;
    private final long windowMs;
    private final int threshold;
    private final Queue<Long> events;

    public TimeWindowRuleNode(String id, Predicate<Message> condition, long windowMs, int threshold) {
        super(id);
        this.condition = condition;
        this.windowMs = windowMs;
        this.threshold = threshold;
        this.events = new LinkedList<>();

        addInputPort("in");
        addOutputPort("alert");
        addOutputPort("pass");
    }

    @Override
    protected void onProcess(Message message) {
        long currentTime = System.currentTimeMillis();
        boolean isConditionMet = condition.test(message);

        synchronized (events) {
            if (isConditionMet) {
                events.add(currentTime);
            }
            while (!events.isEmpty() && (currentTime - events.peek()) > windowMs) {
                events.poll();
            }
            if (events.size() >= threshold) {
                log.debug("[{}] 타임 윈도우 알림 조건 충족! ({}ms 내 {}회 이상)", getId(), windowMs, threshold);
                send("alert", message);
            } else {
                send("pass", message);
            }
        }
    }
}
