package com.fbp.engine.node;

import com.fbp.engine.message.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class LogNode extends AbstractNode {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    public LogNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        String time = LocalDateTime.now().format(FORMATTER);
        System.out.println(String.format("[%s][%s] %s", time, getId(), message));
        send("out", message);
    }
}
