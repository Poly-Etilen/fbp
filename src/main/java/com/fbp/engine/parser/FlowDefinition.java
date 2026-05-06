package com.fbp.engine.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class FlowDefinition {
    private String id;
    private String name;
    private String description;
    private List<NodeDefinition> nodes;
    private List<ConnectionDefinition> connections;

    public List<NodeDefinition> getNodes() {
        return nodes == null ? Collections.emptyList() : Collections.unmodifiableList(nodes);
    }

    public List<ConnectionDefinition> getConnections() {
        return connections == null ? Collections.emptyList() : Collections.unmodifiableList(connections);
    }

    public NodeDefinition getNode(String id) {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(node -> node.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void validate() {
        if (nodes == null || connections == null) return;

        Map<String, NodeDefinition> nodeMap = nodes.stream()
                .collect(Collectors.toMap(NodeDefinition::getId, node -> node));

        for (ConnectionDefinition conn : connections) {
            String fromId = conn.getFrom().split(":")[0];
            String toId = conn.getTo().split(":")[0];

            if (!nodeMap.containsKey(fromId)) {
                throw new IllegalStateException("연결 오류: 존재하지 않는 소스 노드 (" + fromId + ")");
            }
            if (!nodeMap.containsKey(toId)) {
                throw new IllegalStateException("연결 오류: 존재하지 않는 타겟 노드 (" + toId + ")");
            }
        }
    }
}
