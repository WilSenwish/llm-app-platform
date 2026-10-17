package com.llmplatform.core.plugin;

import java.util.Map;

/**
 * 统一的插件入参契约。
 * <p>平台内核以该结构向插件传递任务上下文与负载，所有插件遵循同一入参规范，实现解耦。
 */
public record PluginRequest(
        /** 全链路追踪 ID，贯穿网关→内核→插件→子 Agent。 */
        String traceId,
        /** 发起任务的用户标识，用于配额 / 限流 / 多租户隔离。 */
        String userId,
        /** 任务类型，插件内部据此路由到具体能力。 */
        String taskType,
        /** 业务负载，由插件自定义解析。 */
        Map<String, Object> payload) {
}
