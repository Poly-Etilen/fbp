package com.fbp.engine.plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginClassLoaderTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test-classloader-");
    }

    @AfterEach
    void tearDown() {
        File dir = tempDir.toFile();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            dir.delete();
        }
    }

    @Test
    @DisplayName("JAR 로드")
    void testLoadJar() throws IOException {
        Path jarPath = tempDir.resolve("test.jar");
        Files.createFile(jarPath);
        
        URL[] urls = { jarPath.toUri().toURL() };
        PluginClassLoader loader = new PluginClassLoader(urls, ClassLoader.getSystemClassLoader());
        
        URL[] loadedUrls = loader.getURLs();
        assertEquals(1, loadedUrls.length);
        assertTrue(loadedUrls[0].toString().contains("test.jar"));
    }

    @Test
    @DisplayName("클래스 격리")
    void testClassIsolation() {
        URL[] urls = new URL[0];
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        PluginClassLoader loader = new PluginClassLoader(urls, parent);

        assertDoesNotThrow(() -> loader.loadClass("java.lang.String"));
    }

    @Test
    @DisplayName("리소스 해제")
    void testResourceRelease() throws IOException {
        Path jarPath = tempDir.resolve("resource.jar");
        Files.createFile(jarPath);
        
        PluginClassLoader loader = new PluginClassLoader(new URL[]{ jarPath.toUri().toURL() }, null);
        assertDoesNotThrow(loader::close);
    }

    @Test
    @DisplayName("존재하지 않는 JAR")
    void testNonExistentJar() throws MalformedURLException {
        URL badUrl = new File("wrong-path/ghost.jar").toURI().toURL();
        PluginClassLoader loader = new PluginClassLoader(new URL[]{ badUrl }, null);

        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("com.ghost.UnknownClass"));
    }
}