package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface Node {
    String getId();
    void initialize();
    void process(Message message);
    void shutdown();
}
