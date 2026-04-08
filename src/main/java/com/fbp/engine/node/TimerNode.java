package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimerNode extends AbstractNode {
    private final long intervalMs;
    private int tickCount = 0;
    private ScheduledExecutorService scheduler;

    public TimerNode(String id, long intervalMs) {
        super(id);
        this.intervalMs = intervalMs;
        addOutputPort("out");
    }

    @Override
    public void initialize() {
        log.info("[{}] Timer 시작 (주기: {}ms)", getId(), intervalMs);
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            Message msg = new Message(Map.of(
                    "tick", tickCount++,
                    "timestamp", System.currentTimeMillis()
            ));
            log.debug("[{}] Timer 틱 발생: {}", getId(), tickCount - 1);
            send("out", msg);
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("[{}] Timer 종료", getId());
        }
    }

    @Override
    protected void onProcess(Message message) {

    }
}
