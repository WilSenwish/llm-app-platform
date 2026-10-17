package com.llmplatform.core.plugin;

import io.micrometer.observation.ObservationRegistry;

/**
 * 插件运行时上下文。
 * <p>平台内核在调用插件前注入，向插件提供链路追踪埋点钩子、模型网关客户端、MCP 注册中心等
 * 底座能力。插件不应持有平台内部可变状态，所有底座能力均通过该接口获取（控制反转）。
 */
public interface PluginContext {

    /** 全链路追踪 ID，贯穿网关→内核→插件→子 Agent。 */
    String getTraceId();

    /** 发起任务的用户标识，用于配额 / 限流 / 多租户隔离。 */
    String getUserId();

    /** Micrometer 观测注册中心，插件内部埋点可基于它创建嵌套 Span（全链路可观测）。 */
    ObservationRegistry getObservationRegistry();

    // TODO(week2): ModelGatewayClient getModelGateway();   // 独立 AI 模型网关客户端
    // TODO(week1): McpRegistry getMcpRegistry();           // MCP 工具注册中心
}
