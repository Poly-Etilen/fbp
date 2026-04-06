package com.fbp.engine.core;

import com.fbp.engine.node.AbstractNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class Flow {
    private final String id;
    private final Map<String, AbstractNode> nodes = new HashMap<>();
    @Getter
    private final List<Connection> connections = new ArrayList<>();
    private final Map<String, List<String>> adjList = new HashMap<>();

    public Flow(String id) {
        this.id = id;
    }

    public Flow addNode(AbstractNode node) {
        nodes.put(node.getId(), node);
        adjList.putIfAbsent(node.getId(), new ArrayList<>());
        return this;
    }

    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort) {
        AbstractNode fromNode = nodes.get(sourceNodeId);
        AbstractNode toNode = nodes.get(targetNodeId);

        if (fromNode == null || toNode == null) {
            throw new IllegalArgumentException("존재하지 않는 노드입니다.");
        }

        OutputPort outputPort = fromNode.getOutputPort(sourcePort);
        InputPort inputPort = toNode.getInputPort(targetPort);

        if (outputPort == null || inputPort == null) {
            throw new IllegalArgumentException(String.format("포트 연결 실패: %s(%s) -> %s(%s)", sourceNodeId, sourcePort, targetNodeId, targetPort));
        }
        Connection conn = new Connection();
        outputPort.connect(conn);
        inputPort.connect(conn);
        connections.add(conn);

        adjList.get(sourceNodeId).add(targetNodeId);

        log.debug("Connected: {}[{}] -> {}[{}]",  sourceNodeId, sourcePort, targetNodeId, targetPort);
        return this;
    }

    public void initialize() {
        for (AbstractNode node : nodes.values()) {
            node.initialize();
        }
    }

    public void shutdown() {
        for (AbstractNode node : nodes.values()) {
            node.shutdown();
        }
    }

    public Collection<AbstractNode> getNodes() {
        return nodes.values();
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (nodes.isEmpty()) {
            errors.add("플로우에 등록된 노드가 0개입니다.");
            return errors;
        }

    }
}
