package com.fbp.engine.bridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.engine.message.Message;

public class MessageSerializer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    public byte[] serialize(Message message) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }

    public Message deserialize(byte[] payload) {
        try {
            return objectMapper.readValue(payload, Message.class);
        } catch (Exception e) {
            throw new RuntimeException("메시지 역직렬화 실패", e);
        }
    }
}
