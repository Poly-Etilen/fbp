package com.fbp.engine.core;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.strategy.BackPressureStrategy;
import com.fbp.engine.message.Message;

public interface Connection {
    String getId();
    void deliver(Message message);
    Message poll() throws InterruptedException;
    int getBufferSize();
    void push(Message message);

    InputPort getInputPort();
    void setInputPort(InputPort inputPort);

    InputPort getTarget();
    void setTarget(InputPort target);

    void setStrategy(BackPressureStrategy strategy);
    default void close() {}
}
