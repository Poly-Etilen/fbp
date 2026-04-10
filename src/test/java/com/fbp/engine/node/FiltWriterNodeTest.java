package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class FiltWriterNodeTest {
    private final String TEST_FILE_PATH = "test_output.txt";
    private FileWriterNode node;

    @BeforeEach
    void setUp() throws IOException {
        Files.deleteIfExists(Path.of(TEST_FILE_PATH));
        node = new FileWriterNode("file-writer-test", TEST_FILE_PATH);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (node != null) {
            node.shutdown();
        }
        Files.deleteIfExists(Path.of(TEST_FILE_PATH));
    }

    @Test
    @DisplayName("파일 생성")
    void test1() {
        node.initialize();
        Assertions.assertTrue(Files.exists(Path.of(TEST_FILE_PATH)));
    }

    @Test
    @DisplayName("내용 기록")
    void test2() throws IOException {
        node.initialize();

        node.process(new Message(Map.of("data", "line1")));
        node.process(new Message(Map.of("data", "line2")));
        node.process(new Message(Map.of("data", "line3")));

        node.shutdown();

        List<String> lines = Files.readAllLines(Path.of(TEST_FILE_PATH));
        Assertions.assertEquals(3, lines.size());
        Assertions.assertTrue(lines.getFirst().contains("line1"));
    }

    @Test
    @DisplayName("shutdown 후 파일 닫힘")
    void test3() throws IOException {
        node.initialize();
        node.process(new Message(Map.of("data", "before")));
        node.shutdown();

        Assertions.assertDoesNotThrow(() -> node.process(new Message(Map.of("data", "after"))));

        List<String> lines = Files.readAllLines(Path.of(TEST_FILE_PATH));
        Assertions.assertEquals(1, lines.size());
    }

}
