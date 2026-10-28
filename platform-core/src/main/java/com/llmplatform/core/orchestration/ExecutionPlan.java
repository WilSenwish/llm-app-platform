package com.llmplatform.core.orchestration;

import com.llmplatform.core.plugin.PluginRequest;

/**
 * 前置编排产物：已经决定由哪个插件执行哪次请求。
 *
 * @param pluginId    目标插件 ID
 * @param request     原始请求，执行层不再改写
 * @param routeReason 命中原因，便于日志和测试断言（explicit-plugin-id / task-prefix）
 */
public record ExecutionPlan(String pluginId, PluginRequest request, String routeReason) {
}
