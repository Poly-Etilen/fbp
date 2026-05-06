package com.fbp.engine.parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class JsonFlowParser implements FlowParser {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public FlowDefinition parse(InputStream stream) throws FlowParserException {
        try {
            FlowDefinition definition = mapper.readValue(stream, FlowDefinition.class);
            validate(definition);
            return definition;
        } catch (Exception e) {
            throw new FlowParserException("JSON 플로우 정의 파싱 실패", e);
        }
    }

    private void validate(FlowDefinition definition) {
        if (definition.getId() == null || definition.getId().isBlank()) {
            throw new FlowParserException("플로우 ID는 필수임");
        }
        if (definition.getNodes() == null || definition.getNodes().isEmpty()) {
            throw new FlowParserException("노드 목록이 비어있음");
        }
    }
}
