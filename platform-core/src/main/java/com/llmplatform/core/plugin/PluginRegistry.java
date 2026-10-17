package com.llmplatform.core.plugin;

import java.util.List;
import java.util.Optional;

/**
 * 插件注册中心。
 * <p>管理插件的注册、发现、动态加载与卸载，是平台内核与插件之间的唯一枢纽。
 * <p>P0 提供内存实现；week1 将扩展为基于 SPI（ServiceLoader）的动态加载 / 卸载，
 * 支持运行时热插拔插件包（独立 jar）。
 */
public interface PluginRegistry {

    /** 注册插件（已注册则覆盖并记录告警）。 */
    void register(AppPlugin plugin);

    /** 卸载插件并触发其 destroy 释放资源。 */
    void unregister(String pluginId);

    /** 按插件 ID 获取插件实例。 */
    Optional<AppPlugin> get(String pluginId);

    /** 是否存在指定插件。 */
    boolean contains(String pluginId);

    /** 获取全部已注册插件（用于编排层遍历与治理面板）。 */
    List<AppPlugin> getAll();

    /** 已注册插件数量。 */
    int size();
}
