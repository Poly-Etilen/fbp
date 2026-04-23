package com.fbp.engine.registry;

import com.fbp.engine.core.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class NodeRegistry {
    private final Map<String, NodeFactory> factories = new HashMap<>();

    public void register(String typeName, NodeFactory factory) {
        if (typeName == null || typeName.isBlank()) {
            throw new NodeRegistryException("노드 타입 이름은 비어있을 수 없음");
        }
        if (factory == null) {
            throw new NodeRegistryException("NodeFactory는 null일 수 없음");
        }
        factories.put(typeName, factory);
    }

    public Node create(String typeName, String id, Map<String, Object> config) {
        NodeFactory factory = factories.get(typeName);
        if (factory == null) {
            throw new NodeRegistryException("등록되지 않은 노드 타입: " + typeName);
        }

        try {
            return factory.create(id, config);
        } catch (Exception e) {
            throw new NodeRegistryException("노드 생성 중 오류 발생 (type: " + typeName + ", ID: " + id + ")", e);
        }
    }

    public boolean isRegistered(String typeName) {
        return factories.containsKey(typeName);
    }

    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(factories.keySet());
    }

    public void clear() {
        factories.clear();
    }
}
