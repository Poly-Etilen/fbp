package com.fbp.engine.node;

import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerNode extends AbstractNode{
    private final long intervalMs;
    private int count = 0;
    private ScheduledExecutorService scheduler;

    public TimerNode(String id, long intervalMs) {
        super(id);
        this.intervalMs = intervalMs;
    }

    @Override
    public void initialize() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            String payloadData = (count % 2 == 0) ? "hello" : "ignore";
            Message msg = new Message(Map.of(
                    "data", payloadData,
                    "tick", ++count,
                    "timestamp", System.currentTimeMillis()
            ));
            send("output", msg);
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onProcess(Message message) {

    }

    @Override
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.shutdown();
    }

    @Override
    public void run() {
        initialize();
        try {
            OutputPort output = outputPorts.get("output");
            while (!Thread.currentThread().isInterrupted()) {
                if (output != null) {
                    String payloadData = (count % 2 == 0) ? "hello" : "ignore";
                    Message msg = new Message(Map.of("data", payloadData));
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
