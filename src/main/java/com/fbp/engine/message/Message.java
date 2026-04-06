package com.fbp.engine.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@Getter
public class Message {
    private final String id;
    private final Map<String, Object> payload;
    private final long timestamp;
}
