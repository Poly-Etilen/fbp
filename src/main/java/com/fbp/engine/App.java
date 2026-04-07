package com.fbp.engine;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class App {
    public static void main(String[] args) {
        Message msg = new Message(Map.of("temperature", 25.5, "sensorId", "sensor-A"));
        Message modifiedMsg = msg.withEntry("status", "NORMAL");

        PrintNode printer = new PrintNode("printer-1");

        log.info("--- 원본 메시지 출력 ---");
        printer.process(msg);
        log.info("--- 수정된 메시지 출력 ---");
        printer.process(modifiedMsg);
    }
}
