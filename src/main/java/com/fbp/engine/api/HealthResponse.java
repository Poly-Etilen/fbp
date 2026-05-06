package com.fbp.engine.api;

public record HealthResponse(
        String status,
        long uptime,
        int flowCount
){}
