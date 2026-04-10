package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectorNode extends AbstractNode {
    @Getter
    private final List<Message> collected;

    public CollectorNode(String id) {
        super(id);
        this.collected = Collections.synchronizedList(new ArrayList<>());
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        collected.add(message);
    }
}
