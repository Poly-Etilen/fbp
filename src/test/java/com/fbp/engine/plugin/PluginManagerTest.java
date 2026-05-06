package com.fbp.engine.plugin;

import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    private NodeRegistry registry;
    private PluginManager manager;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException{
        registry = new NodeRegistry();
        PluginScanner scanner = new PluginScanner();
        manager = new PluginManager(registry, scanner);

        tempDir = Files.createTempDirectory("test-manager-");
    }

    @AfterEach
    void tearDown() {
        File dir = tempDir.toFile();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    @DisplayName("ClassPath 플러그인 로드")
    void testClassPathLoad() throws IOException {
        Files.createFile(tempDir.resolve("dummy.jar"));
        
        manager.loadPlugins(tempDir.toString());
        assertTrue(registry.isRegistered("TestPluginType"));
    }

    @Test
    @DisplayName("외부 JAR 로드")
    void testExternalJarDiscovery() throws IOException {
        Files.createFile(tempDir.resolve("plugin-discovery.jar"));
        manager.loadPlugins(tempDir.toString());

        assertFalse(registry.getRegisteredTypes().isEmpty());
    }

    @Test
    @DisplayName("NodeRegistry 자동 등록")
    void testNodeRegistryAutoRegistration() throws IOException {
        Files.createFile(tempDir.resolve("plugin-registry.jar"));
        manager.loadPlugins(tempDir.toString());

        String expectedType = "TestPluginType";
        assertTrue(registry.isRegistered(expectedType));

        assertNotNull(registry.create(expectedType, "test-node", null));
    }

    @Test
    @DisplayName("타입 충돌 처리")
    void testTypeConflictResolution() throws IOException {
        registry.register("TestPluginType", (id, conf) -> null);
        Files.createFile(tempDir.resolve("conflict-plugin.jar"));

        assertDoesNotThrow(() -> manager.loadPlugins(tempDir.toString()));
    }

    @Test
    @DisplayName("잘못된 JAR")
    void testInvalidJarContinues() throws IOException {
        Path badJar = tempDir.resolve("corrupted.jar");
        Files.writeString(badJar, "손상된 파일입니다.");

        assertDoesNotThrow(() -> manager.loadPlugins(tempDir.toString()));
    }

    @Test
    @DisplayName("plugins 디렉토리 없음")
    void testNoDirectory() {
        assertDoesNotThrow(() -> manager.loadPlugins("non-existent-dir"));
        assertFalse(registry.isRegistered("TestPluginType"));
    }

    @Test
    @DisplayName("빈 plugins 디렉토리")
    void testEmptyDirectory() {
        manager.loadPlugins(tempDir.toString());
        assertFalse(registry.isRegistered("TestPluginType"));
    }

    @Test
    @DisplayName("플러그인 수 확인")
    void test8_PluginCount() throws IOException {
        Files.createFile(tempDir.resolve("plugin1.jar"));
        Files.createFile(tempDir.resolve( "plugin2.jar"));
        
        manager.loadPlugins(tempDir.toString());
        assertFalse(registry.getRegisteredTypes().isEmpty());
    }
}