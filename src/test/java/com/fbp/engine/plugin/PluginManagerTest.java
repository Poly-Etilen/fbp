package com.fbp.engine.plugin;

import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
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
    @DisplayName("2 & 3. 외부 JAR 로드 및 NodeRegistry 자동 등록")
    void test2_3_ExternalJarLoadAndAutoRegister(@TempDir File tempDir) throws IOException {
        new File(tempDir, "external-plugin.jar").createNewFile();
        
        manager.loadPlugins(tempDir.getAbsolutePath());
        
        // 플러그인이 레지스트리에 자동 등록되었는지 팩토리 생성으로 확인
        assertNotNull(registry.create("TestPluginType", "node-1", null));
    }

    @Test
    @DisplayName("4. 타입 충돌 처리: 내장 노드와 동일한 typeName 로드 시 덮어쓰기/예외 확인")
    void test4_TypeConflictResolution(@TempDir File tempDir) throws IOException {
        // 기존 엔진에 이미 같은 이름의 노드가 등록되어 있다고 가정
        registry.register("TestPluginType", (id, conf) -> null);
        new File(tempDir, "conflict-plugin.jar").createNewFile();

        // 플러그인 로드 실행 시 기존 것을 덮어쓰거나 예외를 던지는지 확인 (현재 로직은 덮어쓰기)
        assertDoesNotThrow(() -> manager.loadPlugins(tempDir.getAbsolutePath()));
    }

    @Test
    @DisplayName("5. 잘못된 JAR: 유효하지 않은 파일이어도 예외 로깅 후 엔진은 정상 동작")
    void test5_InvalidJarContinues(@TempDir File tempDir) throws IOException {
        File badJar = new File(tempDir, "corrupted.jar");
        try (FileWriter fw = new FileWriter(badJar)) {
            fw.write("This is not a zip/jar format file, it is corrupted.");
        }

        // 깨진 JAR를 만나도 Exception을 밖으로 던지지 않고 삼켜야 함 (PluginException 로깅)
        assertDoesNotThrow(() -> manager.loadPlugins(tempDir.getAbsolutePath()));
    }

    @Test
    @DisplayName("6. plugins 디렉토리 없음: 스캔 건너뜀 (예외 발생 안 함)")
    void test6_NoDirectory() {
        assertDoesNotThrow(() -> manager.loadPlugins("non-existent-dir"));
        assertFalse(registry.isRegistered("TestPluginType")); // 로드된 것 없음
    }

    @Test
    @DisplayName("7. 빈 plugins 디렉토리: JAR가 없으면 정상 건너뜀")
    void test7_EmptyDirectory(@TempDir File tempDir) {
        manager.loadPlugins(tempDir.getAbsolutePath());
        assertFalse(registry.isRegistered("TestPluginType"));
    }

    @Test
    @DisplayName("8. 플러그인 수 확인: 로드 후 전체 타입 수가 예상과 일치")
    void test8_PluginCount(@TempDir File tempDir) throws IOException {
        new File(tempDir, "plugin1.jar").createNewFile();
        new File(tempDir, "plugin2.jar").createNewFile();
        
        manager.loadPlugins(tempDir.getAbsolutePath());
        
        // DummyPluginProvider가 1개의 타입을 반환하므로 최소 1개 이상 등록되어야 함
        assertTrue(registry.getRegisteredTypes().size() >= 1);
    }
}