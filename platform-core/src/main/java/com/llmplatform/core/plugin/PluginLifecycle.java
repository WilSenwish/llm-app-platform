package com.llmplatform.core.plugin;

import com.llmplatform.core.model.ModelGateway;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 插件生命周期：注册并初始化。
 * <p>同一插件 ID 以 Spring 管理的实例为准，SPI 再发现同 ID 时跳过，避免两套向量库各跑各的。
 */
@Slf4j
@Component
public class PluginLifecycle {

    private final PluginRegistry registry;
    private final ObservationRegistry observationRegistry;
    private final List<AppPlugin> springPlugins;
    private final PluginLoader pluginLoader;
    private final ModelGateway modelGateway;
    private final AtomicBoolean started = new AtomicBoolean(false);

    /** 测试与无网关场景使用。模型网关为空时，插件走不含生成模型的路径。 */
    public PluginLifecycle(
            PluginRegistry registry,
            ObservationRegistry observationRegistry,
            List<AppPlugin> springPlugins,
            PluginLoader pluginLoader) {
        this(registry, observationRegistry, springPlugins, pluginLoader, (ModelGateway) null);
    }

    /** 容器启动时注入网关。没有 ModelGateway Bean 时 getIfAvailable 为 null。 */
    @Autowired
    public PluginLifecycle(
            PluginRegistry registry,
            ObservationRegistry observationRegistry,
            List<AppPlugin> springPlugins,
            PluginLoader pluginLoader,
            ObjectProvider<ModelGateway> modelGateways) {
        this(registry, observationRegistry, springPlugins, pluginLoader, modelGateways.getIfAvailable());
    }

    private PluginLifecycle(
            PluginRegistry registry,
            ObservationRegistry observationRegistry,
            List<AppPlugin> springPlugins,
            PluginLoader pluginLoader,
            ModelGateway modelGateway) {
        this.registry = registry;
        this.observationRegistry = observationRegistry;
        this.springPlugins = List.copyOf(springPlugins);
        this.pluginLoader = pluginLoader;
        this.modelGateway = modelGateway;
    }

    /** Spring 容器就绪后自动引导；测试里也可以直接调用。重复调用无效果。 */
    @PostConstruct
    public void bootstrap() {
        if (!started.compareAndSet(false, true)) {
            log.info("[PluginLifecycle] 已完成引导，忽略重复调用");
            return;
        }
        PluginContext context = new DefaultPluginContext("bootstrap", "system", observationRegistry, modelGateway);
        for (AppPlugin plugin : springPlugins) {
            registerAndInit(plugin, context, "spring");
        }
        for (AppPlugin plugin : pluginLoader.load()) {
            String pluginId = plugin.getMetadata().id();
            if (registry.contains(pluginId)) {
                log.info("[PluginLifecycle] 跳过 SPI 重复插件 id={}", pluginId);
                continue;
            }
            registerAndInit(plugin, context, "spi");
        }
    }

    private void registerAndInit(AppPlugin plugin, PluginContext context, String source) {
        registry.register(plugin);
        plugin.init(context);
        log.info("[PluginLifecycle] 已初始化 source={} id={}", source, plugin.getMetadata().id());
    }
}
