package com.fbp.engine.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.Flow;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.core.Node;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.registry.NodeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class FlowManager {
    private final NodeRegistry registry;
    private final FlowEngine flowEngine;
    private final Map<String, Flow> activeFlows = new ConcurrentHashMap<>();

    public void deploy(FlowDefinition definition) {
        if (activeFlows.containsKey(definition.getId())) {
            throw new RuntimeException("이미 동일한 ID의 플로우가 존재함: " + definition.getId());
        }
        log.info("플로우 배포 시작: {} ({})",definition.getName(), definition.getId());

        Map<String, Node> nodeInstance = new HashMap<>();
        for (NodeDefinition nodeDef : definition.getNodes()) {
            Node node = registry.create(nodeDef.getType(), nodeDef.getId(), nodeDef.getConfig());
            nodeInstance.put(node.getId(), node);
        }

        for (ConnectionDefinition connDef : definition.getConnections()) {
            String[] fromParts = connDef.getFrom().split(":");
            String[] toPart = connDef.getTo().split(":");

            if (fromParts.length < 2 || toPart.length < 2) {
                throw new RuntimeException("잘못된 연결 방식임: " + connDef.getFrom() + " -> " + connDef.getTo());
            }

            Node sourceNode = nodeInstance.get(fromParts[0]);
            Node targetNode = nodeInstance.get(toPart[0]);

            if (sourceNode == null || targetNode == null) {
                throw new RuntimeException("연결할 노드를 찾을 수 없습니다.");
            }
            if (sourceNode instanceof AbstractNode && targetNode instanceof AbstractNode) {
                AbstractNode outNode = (AbstractNode) sourceNode;
                AbstractNode inNode = (AbstractNode) targetNode;

                Connection conn = new Connection();
                outNode.getOutputPort(fromParts[1]).connect(conn);
                inNode.getInputPort(toPart[1]).connect(conn);
            }
        }
    }
}
