package com.llmplatform.core.plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于内存的插件注册中心实现（P0）。
 * <p>使用 ConcurrentHashMap 保证并发安全；week1 将扩展为 SPI ServiceLoader 动态加载 / 卸载，
 * 支持运行时热插拔独立插件 jar。
 */
@Slf4j
@Component
public class InMemoryPluginRegistry implements PluginRegistry {

    private final Map<String, AppPlugin> plugins = new ConcurrentHashMap<>();

    @Override
    public void register(AppPlugin plugin) {
        PluginMetadata meta = plugin.getMetadata();
        if (plugins.containsKey(meta.id())) {
            log.warn("[PluginRegistry] 插件 id={} 已存在，将被覆盖", meta.id());
        }
        plugins.put(meta.id(), plugin);
        log.info("[PluginRegistry] 注册插件 id={} name={} type={}", meta.id(), meta.name(), meta.type());
    }

    @Override
    public void unregister(String pluginId) {
        AppPlugin removed = plugins.remove(pluginId);
        if (removed != null) {
            try {
                removed.destroy();
            } catch (Exception e) {
                log.warn("[PluginRegistry] 卸载插件 id={} 时 destroy 异常", pluginId, e);
            }
            log.info("[PluginRegistry] 卸载插件 id={}", pluginId);
        }
    }

    @Override
    public Optional<AppPlugin> get(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    @Override
    public boolean contains(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    @Override
    public List<AppPlugin> getAll() {
        return List.copyOf(plugins.values());
    }

    @Override
    public int size() {
        return plugins.size();
    }
}
