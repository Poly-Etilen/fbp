package com.fbp.engine.node;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.Node;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractNode implements Node {
    protected String id;
    protected Map<String, InputPort> inputPorts = new HashMap<>();
    protected Map<String, OutputPort> outputPorts = new HashMap<>();

    public AbstractNode(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void addInputPort(String name, InputPort inputPort) {
        inputPorts.put(name, inputPort);
    }
    public void addOutputPort(String name, OutputPort outputPort) {
        outputPorts.put(name, outputPort);
    }

    protected abstract void onProcess(Message message);

    @Override
    public void initialize() {
        log.debug("Initializing node {}", id);
    }

    @Override
    public void process(Message message) {
        log.debug("Processing node {}", id);
        onProcess(message);
    }

    @Override
    public void shutdown() {
        log.debug("Shutting down node {}", id);
    }

    @Override
    public void run() {
        initialize();
        try {
            InputPort inputPort = inputPorts.get("input");
            if (inputPort != null) {
                while (!Thread.currentThread().isInterrupted()) {
                    Message msg = inputPort.read();
                    if (msg != null) {
                        process(msg);
                    }
                }
            }
        } finally {
            shutdown();
        }
    }
}
