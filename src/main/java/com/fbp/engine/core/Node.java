package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface Node extends Runnable{
    String getId();
    void process(Message message);
}
