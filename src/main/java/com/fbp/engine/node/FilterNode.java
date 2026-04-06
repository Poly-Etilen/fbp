package com.fbp.engine.node;

import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;

public class FilterNode extends AbstractNode{
    private final String key;
    private final double threshold;

    public FilterNode(String id, String key, double threshold) {
        super(id);
        this.key = key;
        this.threshold = threshold;
    }

    @Override
    protected void onProcess(Message message) {
        if (message.hasKey(key)) {
            Double value = message.get(key);
            if (value != null && value >= threshold) {
                send("out", message);
            }
        }
    }
}
