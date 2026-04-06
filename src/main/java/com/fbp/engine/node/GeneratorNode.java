package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class GeneratorNode implements Node {
    private final String id;
    private final OutputPort output;
    private int count = 0;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void process(Message message) {
        output.send(message);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Message msg = new Message("message-" + (++count), Map.of("data", "Hello from Generator " + count), System.currentTimeMillis());
                output.send(msg);
                Thread.sleep(1000);
            }
        }catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
