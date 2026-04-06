//package com.fbp.engine.core;
//
//import com.fbp.engine.node.AbstractNode;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//public class Flow {
//    private final String id;
//    private final Map<String, Node> nodes = new HashMap<>();
//
//    private final Map<String, List<String>> adjList = new HashMap<>();
//    private final Map<String, Integer> inDegree = new HashMap<>();
//
//    public Flow(String id) {
//        this.id = id;
//    }
//
//    public void addNode(Node node) {
//        nodes.put(node.getId(), node);
//        adjList.putIfAbsent(node.getId(), new ArrayList<>());
//        inDegree.putIfAbsent(node.getId(), 0);
//    }
//
//    public void connect(String fromNodeId, String fromPortName, String toNodeId, String toPortName) {
//        AbstractNode fromNode = (AbstractNode) nodes.get(fromNodeId);
//        AbstractNode toNode = (AbstractNode) nodes.get(toNodeId);
//
//        if (fromNode == null || toNode == null) {
//            throw new IllegalArgumentException("존재하지 않는 노드입니다.");
//        }
//
//        OutputPort outputPort = fromNode.getOutputPort(fromPortName);
//        InputPort inputPort = toNode.getInputPort(toPortName);
//
//        if (outputPort == null || inputPort == null) {
//            throw new IllegalArgumentException(String.format("포트 연결 실패: %s(%s) -> %s(%s)", fromNodeId, fromPortName, toNodeId, toPortName));
//        }
//    }
//
//}
