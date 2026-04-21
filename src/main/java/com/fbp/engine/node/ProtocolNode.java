package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.ConnectionState;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ProtocolNode extends AbstractNode{
    protected final Map<String, Object> config;
    @Getter
    protected ConnectionState connectionState = ConnectionState.DISCONNECTED;
    protected long reconnectIntervalMs;

    private ScheduledExecutorService reconnectScheduler;

    public ProtocolNode(String id, Map<String, Object> config) {
        super(id);
        this.config = config;
        this.reconnectIntervalMs = (long) config.getOrDefault("reconnectIntervalMs", 5000L);
    }

    @Override
    public void initialize() {
        super.initialize();
        this.connectionState = ConnectionState.CONNECTING;
        tryConnect();
    }

    @Override
    public void shutdown() {
        if (reconnectScheduler != null && !reconnectScheduler.isShutdown()) {
            reconnectScheduler.shutdownNow();
        }
        disconnect();
        this.connectionState = ConnectionState.DISCONNECTED;
        log.info("[{}] 프로토콜 노드 종료 맟 연결 해제", getId());
        super.shutdown();
    }

    private void tryConnect() {
        try {
            connect();
            this.connectionState = ConnectionState.CONNECTED;
            log.info("[{}] 네트워크 연결 성공", getId());
        } catch (Exception e) {
            this.connectionState = ConnectionState.ERROR;
            log.error("[{}] 연결 실패: {}. {}ms 후 재연결 시도", getId(), e.getMessage(), reconnectIntervalMs);
            reconnect();
        }

    }

    protected void reconnect() {
        if (reconnectScheduler == null || reconnectScheduler.isShutdown()) {
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        reconnectScheduler.schedule(this::tryConnect, reconnectIntervalMs, TimeUnit.MILLISECONDS);
    }

    protected abstract void connect() throws Exception;
    protected abstract void disconnect();

    public Object getConfig(String key) {
        return config.get(key);
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }
}
