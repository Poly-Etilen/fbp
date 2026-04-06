package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface InputPort {
    void receive(Message message);
}
