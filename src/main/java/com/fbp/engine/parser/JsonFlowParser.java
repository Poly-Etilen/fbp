package com.fbp.engine.parser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

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

        Set<String> nodeIds = new HashSet<>();
        for (NodeDefinition node : definition.getNodes()) {
            if (!nodeIds.add(node.getId())) {
                throw new FlowParserException("중볻된 노드 ID가 존재함");
            }
        }

        if (definition.getConnections() != null) {
            for (ConnectionDefinition conn : definition.getConnections()) {
                if (conn.getFrom() == null || !conn.getFrom().contains(":")) {
                    throw new FlowParserException("잘못된 연결 형식(from)");
                }
                if (conn.getTo() == null || !conn.getTo().contains(":")) {
                    throw new FlowParserException("잘못된 연결 형식(to)");
                }

                String fromNodeId = conn.getFrom().split(":")[0];
                String toNodeIId = conn.getTo().split(":")[0];

                if (!nodeIds.contains(fromNodeId)) {
                    throw new FlowParserException("존재하지 안는 소스 노드 참조");
                }
                if (!nodeIds.contains(toNodeIId)) {
                    throw new FlowParserException("존재하지 않는 타겟 노드 참조");
                }
            }
        }
    }
}
