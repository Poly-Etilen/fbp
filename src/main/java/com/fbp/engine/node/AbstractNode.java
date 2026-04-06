package com.fbp.engine.node;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.Node;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.core.portImpl.DefaultInputPort;
import com.fbp.engine.core.portImpl.DefaultOutputPort;
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

    public void addInputPort(String name) {
        inputPorts.put(name, new DefaultInputPort(name, this));
    }
    public void addOutputPort(String name) {
        outputPorts.put(name, new DefaultOutputPort(name));
    }

    public InputPort getInput(String name) {
        return inputPorts.get(name);
    }

    public OutputPort getOutput(String name) {
        return outputPorts.get(name);
    }

    protected abstract void onProcess(Message message);

    public InputPort getInputPort(String name) {
        return inputPorts.get(name);
    }
    public OutputPort getOutputPort(String name) {
        return outputPorts.get(name);
    }

    protected void send(String portName, Message message) {
        OutputPort out =  outputPorts. get(portName);
        if (out != null) {
            out.send(message);
        } else  {
            log.warn("No such output port {}", portName);
        }
    }

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
            InputPort in = inputPorts.get("in");
            if (in != null) {
                while (!Thread.currentThread().isInterrupted()) {
                    Message msg = in.read();
                    if (msg != null) {
                        try {
                            process(msg);
                        } catch (Exception e) {
                            log.error("Error processing node {}", id, e);
                        }
                    }
                }
            }
        } finally {
            shutdown();
        }
    }
}
