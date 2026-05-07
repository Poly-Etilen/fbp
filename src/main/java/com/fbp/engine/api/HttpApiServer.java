package com.fbp.engine.api;

import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.metrics.MetricsCollector;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
public class HttpApiServer {
    private final HttpServer server;

    public HttpApiServer(int port, FlowManager flowManager, MetricsCollector metricsCollector)  throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", new HealthHandler(flowManager));
        server.createContext("/flows", new FlowHandler(flowManager, metricsCollector));

        server.createContext("/nodes/", new MetricsHandler(metricsCollector, flowManager));

        server.setExecutor(null);
    }

    public void start() {
        server.start();
        log.info("REST API 서버가 포트 {}에서 실행되었습니다.", server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        log.info("REST API 서버가 종료됨");
    }
}
