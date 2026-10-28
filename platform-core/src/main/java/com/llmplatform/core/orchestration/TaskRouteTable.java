package com.llmplatform.core.orchestration;

import com.llmplatform.core.plugin.PluginRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * taskType 前缀到插件 ID 的路由表。
 * <p>内核只认识前缀约定，不依赖 plugin-rag 模块。{@code payload.pluginId} 是保留字段，显式指定时优先。
 */
@Component
public class TaskRouteTable {

    /** 调用方在 payload 里显式指定插件时使用的保留键。 */
    public static final String PLUGIN_ID_KEY = "pluginId";

    private final List<RouteRule> rules;

    public TaskRouteTable() {
        this(List.of(new RouteRule("rag.", "plugin-rag")));
    }

    public TaskRouteTable(List<RouteRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * 解析目标插件。
     *
     * @return 空表示没有规则命中
     */
    public Optional<RouteHit> resolve(PluginRequest request) {
        Object explicit = request.payload().get(PLUGIN_ID_KEY);
        if (explicit instanceof String pluginId && !pluginId.isBlank()) {
            return Optional.of(new RouteHit(pluginId, "explicit-plugin-id"));
        }
        for (RouteRule rule : rules) {
            if (request.taskType().startsWith(rule.taskPrefix())) {
                return Optional.of(new RouteHit(rule.pluginId(), "task-prefix"));
            }
        }
        return Optional.empty();
    }

    /** 一条前缀规则。前缀需带分隔符，避免 {@code rag} 误匹配 {@code ragtime}。 */
    public record RouteRule(String taskPrefix, String pluginId) {
    }

    /** 一次路由命中。 */
    public record RouteHit(String pluginId, String reason) {
    }
}
