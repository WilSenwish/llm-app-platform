package com.llmplatform.core.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.springframework.stereotype.Component;

/**
 * 基于 JDK {@link ServiceLoader} 的插件发现。
 * <p>插件 jar 在 {@code META-INF/services/com.llmplatform.core.plugin.AppPlugin} 声明实现类。
 * 这是 classpath 级发现，不是运行时热插拔独立 jar（后者留到后续再做）。
 */
@Component
public class SpiPluginLoader implements PluginLoader {

    @Override
    public List<AppPlugin> load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = AppPlugin.class.getClassLoader();
        }
        List<AppPlugin> plugins = new ArrayList<>();
        ServiceLoader.load(AppPlugin.class, classLoader).forEach(plugins::add);
        return List.copyOf(plugins);
    }
}
