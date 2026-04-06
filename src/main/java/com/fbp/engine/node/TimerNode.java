package com.fbp.engine.node;

import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;

import java.util.Map;

public class TimerNode extends AbstractNode{
    private final long intervalMs;
    private int count = 0;

    public TimerNode(String id, long intervalMs) {
        super(id);
        this.intervalMs = intervalMs;
    }

    @Override
    protected void onProcess(Message message) {

    }

    @Override
    public void run() {
        initialize();
        try {
            OutputPort output = outputPorts.get("output");
            while (!Thread.currentThread().isInterrupted()) {
                if (output != null) {
                    String payloadData = (count % 2 == 0) ? "hello" : "ignore";
                    Message msg = new Message("timer-" + (++count), Map.of("data", payloadData), System.currentTimeMillis());
                    output.send(msg);
                }
            }
            Thread.sleep(intervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }
}
