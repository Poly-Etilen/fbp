package com.fbp.engine.core;

import com.fbp.engine.node.AbstractNode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class Flow {
    private final String id;
    private final String name;
    private final Map<String, AbstractNode> nodes = new LinkedHashMap<>();
    private final List<FlowConnection> connections = new ArrayList<>();

    public enum FlowState {STOPPED, RUNNING}
    private enum State {UNVISITED, VISITING, VISITED}
    @Setter
    private FlowState state = FlowState.STOPPED;

    @Getter
    public static class FlowConnection {
        private final String id;
        private final String sourceNodeId;
        private final String targetNodeId;
        private final Connection connection;

        public FlowConnection(String sourceId, String sourcePort, String targetId, String targetPort, Connection connection) {
            this.id = sourceId + ":" + sourcePort + "->" + targetId + ":" + targetPort;
            this.sourceNodeId = sourceId;
            this.targetNodeId = targetId;
            this.connection = connection;
        }
    }

    public Flow(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Flow addNode(AbstractNode node) {
        nodes.put(node.getId(), node);
        return this;
    }
    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort) {
        AbstractNode sourceNode = nodes.get(sourceNodeId);
        AbstractNode targetNode = nodes.get(targetNodeId);
        if (sourceNode == null) {
            throw new IllegalArgumentException("Source node " + sourceNodeId + " does not exist");
        }
        if (targetNode == null) {
            throw new IllegalArgumentException("Target node " + targetNodeId + " does not exist");
        }
        if (sourceNode.getOutputPort(sourcePort) == null) {
            throw new IllegalArgumentException("Source port " + sourcePort + " does not exist");
        }
        if (targetNode.getInputPort(targetPort) == null) {
            throw new IllegalArgumentException("Target port " + targetPort + " does not exist");
        }

        Connection connection = new Connection();
        connection.setTarget(targetNode.getInputPort(targetPort));
        sourceNode.getOutputPort(sourcePort).connect(connection);

        connections.add(new FlowConnection(sourceNodeId, sourcePort, targetNodeId, targetPort, connection));
        return this;
    }

    public void initialize() {
        nodes.values().forEach(AbstractNode::initialize);
    }

    public void shutdown() {
        nodes.values().forEach(AbstractNode::shutdown);
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (nodes.isEmpty()) {
            errors.add("Flow에 등록된 노드가 없습니다.");
        }

        for (FlowConnection connection : connections) {
            if (!nodes.containsKey(connection.getSourceNodeId())) {
                errors.add("연결 오류: 출발지 노드(" + connection.getSourceNodeId() + ")가 존재하지 않음");
            }
            if (!nodes.containsKey(connection.getTargetNodeId())) {
                errors.add("연결 오류: 도착지 노드(" + connection.getTargetNodeId() + ")가 존재하지 않음");
            }
        }

        if (hasCycle()) {
            errors.add("Flow에 순환 연결이 존재하여 무한 루프의 위험 존재");
        }
        return errors;
    }

    private boolean hasCycle() {
        Map<String, List<String>> adjList = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            adjList.put(nodeId, new ArrayList<>());
        }
        for (FlowConnection connection : connections) {
            adjList.get(connection.getSourceNodeId()).add(connection.getTargetNodeId());
        }

        Map<String, State> states = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            states.put(nodeId, State.UNVISITED);
        }

        for (String nodeId : nodes.keySet()) {
            if (states.get(nodeId) == State.UNVISITED) {
                if (dfs(nodeId, adjList, states)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfs(String nodeId, Map<String, List<String>> adjList, Map<String, State> states) {
        states.put(nodeId, State.VISITING);
        for (String neighbor : adjList.get(nodeId)) {
            State neighborState = states.get(neighbor);
            if (neighborState == State.VISITING) {
                return true;
            } else if (neighborState == State.UNVISITED) {
                if (dfs(neighbor, adjList, states)) {
                    return true;
                }
            }
        }
        states.put(nodeId, State.VISITED);
        return false;
    }
}
