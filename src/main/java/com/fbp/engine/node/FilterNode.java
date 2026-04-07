package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultInputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import lombok.Getter;

public class FilterNode implements Node {
    private final String id;
    private final String key;
    private final double threshold;
    @Getter
    private final InputPort inputPort;
    @Getter
    private final OutputPort outputPort;

    public FilterNode(String id, String key, double threshold) {
        this.id = id;
        this.key = key;
        this.threshold = threshold;
        this.inputPort = new DefaultInputPort("in", this);
        this.outputPort = new DefaultOutputPort("out");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {
        if (message.hasKey(key)) {
            Object value = message.get(key);
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                if (numValue >= threshold) {
                    outputPort.send(message);
                }
            }
        }
    }
}
