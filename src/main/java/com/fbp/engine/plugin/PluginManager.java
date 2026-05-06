package com.fbp.engine.plugin;

import com.fbp.engine.registry.NodeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class PluginManager {
    private final NodeRegistry nodeRegistry;
    private final PluginScanner pluginScanner;
    private PluginClassLoader pluginClassLoader;

    public void loadPlugins(String pluginDirPath) {
        List<URL> urls = pluginScanner.scanForJars(pluginDirPath);

        if (urls.isEmpty()) {
            log.info("플러그인 로드를 종료함 (JAR을 찾을 수 없음)");
            return;
        }

        pluginClassLoader = new PluginClassLoader(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader()
        );

        ServiceLoader<NodeProvider> loader = ServiceLoader.load(NodeProvider.class, pluginClassLoader);
        Iterator<NodeProvider> iterator = loader.iterator();

        int loadedCount = 0;
        while (true) {
            try {
                if (!iterator.hasNext()) break;
                NodeProvider provider = iterator.next();

                for (NodeDescriptor descriptor : provider.getNodeDescriptors()) {
                    nodeRegistry.register(descriptor.typeName(), descriptor.factory());
                    log.info("커스텀 노드 플러그인 틍록 완료: {} ({})", descriptor.typeName(), descriptor.description());
                    loadedCount++;
                }
            } catch (ServiceConfigurationError e) {
                PluginException pluginException = new PluginException("유효하지 않는 플러그인 입니다.", e);
                log.error(pluginException.getMessage(), pluginException);
            } catch (Exception e) {
                PluginException pluginException = new PluginException("플러그인 노드 등록 중 에러가 발생함");
                log.error(pluginException.getMessage(), pluginException);
            }
        }
        log.info("총 {}개의 커스텀 노드가 플러그인을 통해 로드됨", loadedCount);
    }
}
