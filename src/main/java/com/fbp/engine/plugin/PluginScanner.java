package com.fbp.engine.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PluginScanner {
    public List<URL> scanForJars(String pluginDirPath) {
        List<URL> urls = new ArrayList<>();
        File pluginDir = new File(pluginDirPath);

        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            log.info("플러그인 디렉토리가 존재하지 않거나 폴더가 아님: {}", pluginDirPath);
            return urls;
        }

        File[] files = pluginDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null || files.length == 0) {
            log.info("디렉토리 내에 로드할 JAR 파일이 없음: {}", pluginDirPath);
            return urls;
        }

        for (File jar : files) {
            try {
                urls.add(jar.toURI().toURL());
                log.debug("플러그인 JAR 탐색 완료: {}", jar.getName());
            } catch (MalformedURLException e) {
                PluginException pluginException = new PluginException("JAR 파일 URL 변환 중 에러 발생: " + jar.getName(), e);
                log.error(pluginException.getMessage(), pluginException);
            }
        }

        return urls;
    }
}
