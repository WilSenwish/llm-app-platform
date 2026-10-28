package com.llmplatform.core.plugin;

import java.util.List;

/**
 * 插件发现端口。
 * <p>进程内 Spring Bean 与 classpath SPI 都通过该端口进入注册中心，调度层不关心插件从哪来。
 */
@FunctionalInterface
public interface PluginLoader {

    /** 加载当前可见的插件实例。每次调用都可以返回新实例。 */
    List<AppPlugin> load();
}
