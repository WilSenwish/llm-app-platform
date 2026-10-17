package com.llmplatform.core.plugin;

/**
 * 插件元信息。
 * <p>描述插件的身份与能力，供插件注册中心统一管理、治理与可观测维度打标。
 */
public record PluginMetadata(
        /** 全局唯一插件 ID，如 plugin-rag。 */
        String id,
        /** 展示名称。 */
        String name,
        /** 语义化版本号。 */
        String version,
        /** 能力描述。 */
        String description,
        /** 插件类型，决定指标维度与隔离策略。 */
        PluginType type) {
}
