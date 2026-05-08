package com.fbp.engine.core.port;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;


public interface OutputPort {
    String getName();
    void connect(Connection connection);
    void send(Message message);
    boolean isConnected();
    void disconnect(Connection connection);
    void disconnectAll();
}
