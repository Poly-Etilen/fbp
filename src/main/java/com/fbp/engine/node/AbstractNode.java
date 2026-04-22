package com.fbp.engine.node;

import com.fbp.engine.core.Node;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultInputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractNode implements Node {
    private final String id;
    private final Map<String, InputPort> inputPorts = new HashMap<>();
    private final Map<String, OutputPort> outputPorts = new HashMap<>();

    public AbstractNode(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void addInputPort(String name) {
        inputPorts.put(name, new DefaultInputPort(name, this));
    }

    public void addOutputPort(String name) {
        outputPorts.put(name, new DefaultOutputPort(name));
    }

    public InputPort getInputPort(String name) {
        return inputPorts.get(name);
    }
    public OutputPort getOutputPort(String name) {
        return outputPorts.get(name);
    }

    protected void send(String portName, Message message) {
        OutputPort port = outputPorts.get(portName);
        if (port != null) {
            port.send(message);
        }
    }

    @Override
    public void initialize() {}

    @Override
    public void shutdown() {}

    @Override
    public void process(Message message) {
//        log.info("[{}] processing message...", id);
        onProcess(message);
    }

    protected abstract void onProcess(Message message);
}
