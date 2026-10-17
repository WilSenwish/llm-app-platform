package com.llmplatform.core.plugin;

/**
 * 插件类型枚举。
 * <p>用于插件元信息登记与插件维度指标区分（Grafana 大盘按 plugin.type 筛选）。
 */
public enum PluginType {
    /** 检索增强生成。 */
    RAG,
    /** 代码分析 / 生成。 */
    CODE_AGENT,
    /** 结构化信息抽取。 */
    EXTRACTOR,
    /** 多 Agent 编排。 */
    MULTI_AGENT,
    /** 自定义业务插件。 */
    CUSTOM
}
