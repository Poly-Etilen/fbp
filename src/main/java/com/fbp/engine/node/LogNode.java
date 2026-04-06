package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PrintNode extends AbstractNode {

    public PrintNode(String id) {
        super(id);
    }

    @Override
    protected void onProcess(Message message) {
        log.debug("[{}] Received Payload: {}", id, message.getPayload());
    }

}
