package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterNode extends AbstractNode {
    private final String key;
    private final double threshold;


    public FilterNode(String id, String key, double threshold) {
        super(id);
        this.key = key;
        this.threshold = threshold;
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        Object value = message.get(key);
        if (value instanceof Number) {
            double doubleValue = ((Number)value).doubleValue();
            if (doubleValue >= threshold) {
                log.info("[{}] 조건 통과: {}", getId(), doubleValue);
                send("out", message);
            } else {
                log.info("[{}] 조건 미달 : {}",  getId(), doubleValue);
            }
        }
    }
}
