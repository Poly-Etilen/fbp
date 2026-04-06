package com.fbp.engine.core;

import com.fbp.engine.message.Message;

public interface InputPort {
    String getName();
    Message read();
    void connect(Connection conn);
}
