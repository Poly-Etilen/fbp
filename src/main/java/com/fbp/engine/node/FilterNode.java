package com.fbp.engine.node;

import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;

public class FilterNode extends AbstractNode{
    private final String keyword;

    public FilterNode(String id, String keyword) {
        super(id);
        this.keyword = keyword;
    }

    @Override
    protected void onProcess(Message message) {
        String data = (String) message.getPayload().get("data");
        if (data != null && data.contains(keyword)) {
            OutputPort output = outputPorts.get("output");
            if (output != null) {
                output.send(message);
            }
        }
    }
}
