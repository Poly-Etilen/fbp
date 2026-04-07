package com.fbp.engine;

import com.fbp.engine.core.Connection;
import com.fbp.engine.node.FilterNode;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.node.PrintNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    public static void main(String[] args) {
        GeneratorNode generator = new GeneratorNode("sensor-A");
        FilterNode filter = new FilterNode("temp-filter", "temperature", 30.0);
        PrintNode printer = new PrintNode("printer-1");

        Connection conn1 = new Connection();
        Connection conn2 = new Connection();

        generator.getOutputPort().connect(conn1);
        conn1.setTarget(filter.getInputPort());

        filter.getOutputPort().connect(conn2);
        conn2.setTarget(printer.getInputPort());

        log.info("--- 테스트 1 (필터 차단) ---");
        generator.generate("temperature", 25.5);

        log.info("--- 테스트 2 필터 통과 ---");
        generator.generate("temperature", 35.5);
    }
}
