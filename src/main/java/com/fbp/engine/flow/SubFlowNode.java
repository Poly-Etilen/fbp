package com.fbp.engine.flow;

import com.fbp.engine.core.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SubFlowNode extends AbstractNode {
    private final Flow internalFlow;
    private final Map<String, String> inputMapping = new HashMap<>();
    private final Map<String, String> outputMapping = new HashMap<>();

    public SubFlowNode(String id, Flow internalFlow) {
        super(id);
        this.internalFlow = internalFlow;
    }

    public void mapInput(String externalPort, String internalNodeId) {
        addInputPort(externalPort);
        inputMapping.put(externalPort, internalNodeId);
    }

    public void mapOutput(String internalPort, String internalNodeId, String externalPort) {
        addOutputPort(externalPort);
        outputMapping.put(internalPort + ":" + internalPort, externalPort);
    }

    @Override
    protected void onProcess(Message message) {
        String targetNodeId = inputMapping.values().stream().findFirst().orElse(null);

        if (targetNodeId != null) {
            AbstractNode targetNode = internalFlow.getNode(targetNodeId);
            if (targetNode != null) {
                targetNode.process(message);
            }
        }
    }

    @Override
    public void initialize() {
        if (internalFlow != null) {
            internalFlow.initialize();
        }
    }

    @Override
    public void shutdown() {
        if (internalFlow != null) {
            internalFlow.shutdown();
        }
    }
}
