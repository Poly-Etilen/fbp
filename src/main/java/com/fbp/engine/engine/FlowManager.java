package com.fbp.engine.engine;

import com.fbp.engine.bridge.MqttBridgeConnection;
import com.fbp.engine.core.*;
import com.fbp.engine.node.AbstractNode;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.FlowDefinition;
import com.fbp.engine.parser.NodeDefinition;
import com.fbp.engine.parser.TransportDefinition;
import com.fbp.engine.registry.NodeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class FlowManager {
    private final NodeRegistry registry;
    private final FlowEngine flowEngine;
    private final Map<String, Flow> activeFlows = new ConcurrentHashMap<>();
    private final Map<String, FlowDefinition> flowDefinitions = new ConcurrentHashMap<>();

    public void deploy(FlowDefinition definition) {
        if (activeFlows.containsKey(definition.getId())) {
            throw new RuntimeException("이미 동일한 ID의 플로우가 존재함: " + definition.getId());
        }
        log.info("플로우 배포 시작: {} ({})",definition.getName(), definition.getId());

        Flow flow = new Flow(definition.getId(), definition.getName());

        for (NodeDefinition nodeDef : definition.getNodes()) {
            Node node = registry.create(nodeDef.getType(), nodeDef.getId(), nodeDef.getConfig());
            flow.addNode((AbstractNode) node);
        }

        TransportDefinition transport = definition.getTransport();
        boolean useMqtt = transport != null && transport.getType().equalsIgnoreCase("mqtt");

        for (ConnectionDefinition connDef : definition.getConnections()) {
            String[] fromParts = connDef.getFrom().split(":");
            String[] toPart = connDef.getTo().split(":");

            if (fromParts.length < 2 || toPart.length < 2) {
                throw new RuntimeException("잘못된 연결 방식임: " + connDef.getFrom() + " -> " + connDef.getTo());
            }

            Connection connection;
            if (useMqtt) {
                String topic = String.format("fbp/%s/%s.%s->%s.%s", definition.getId(), fromParts[0], fromParts[1], toPart[0], toPart[1]);

                connection = new MqttBridgeConnection(transport.getBroker(), topic, transport.getQos());
                log.debug("MQTT Bridge 커넥션 생성 - 토픽: {}", topic);
            } else {
                connection = new LocalConnection();
            }

            flow.connect(fromParts[0], fromParts[1], toPart[0], toPart[1], connection);
        }

        flowEngine.register(flow);
        flowEngine.startFlow(flow.getId());

        activeFlows.put(flow.getId(), flow);
        flowDefinitions.put(definition.getId(), definition);
    }

    public void undeploy(String flowId) {
        if (!activeFlows.containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }

        Flow flow = activeFlows.remove(flowId);
        flowDefinitions.remove(flow.getId());
        if (flow != null) {
            flowEngine.stopFlow(flowId);
        }
    }

    public Flow getFlow(String flowId) {
        return activeFlows.get(flowId);
    }

    public List<String> getDeployedFlowIds() {
        return new ArrayList<>(activeFlows.keySet());
    }

    public Flow.FlowState getFlowStatus(String flowId) {
        Flow flow = activeFlows.get(flowId);
        return (flow != null) ? flow.getState() : null;
    }

    public FlowDefinition getDefinition(String flowId) {
        return flowDefinitions.get(flowId);
    }

    public void stopFlow(String flowId) {
        if (!activeFlows.containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }
        flowEngine.stopFlow(flowId);
        log.info("플로우 정지: {}", flowId);
    }

    public void restartFlow(String flowId) {
        if (!activeFlows.containsKey(flowId)) {
            throw new IllegalArgumentException("존재하지 않는 플로우 ID입니다: " + flowId);
        }
        flowEngine.startFlow(flowId);
        log.info("플로우 재시작: {}", flowId);
    }

    public void update(FlowDefinition newDefinition) {
        log.info("플로우 업데이트 시작: {}", newDefinition.getId());
        undeploy(newDefinition.getId());
        deploy(newDefinition);
    }

    public Collection<Flow> getAllFlows() {
        return activeFlows.values();
    }

    public void remove(String flowId) {
        log.info("플로우 제거 요청: {}", flowId);
        undeploy(flowId);
    }

    public Map<String, Object> getEngineStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("flowCount", activeFlows.size());
        stats.put("deployedFlowIds", getDeployedFlowIds());
        return stats;
    }
}
