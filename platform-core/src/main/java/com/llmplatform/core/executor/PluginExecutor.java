package com.llmplatform.core.executor;

import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 插件执行器（前置编排层骨架）。
 * <p>在调用插件前后自动创建 Micrometer Observation Span，落实“新增任何功能优先增加 OTEL 埋点”
 * 的全链路可观测约定。week4 将 Observation 对接 OpenTelemetry Collector，导出 Trace 与指标。
 * <p>三层执行器（前置编排 → 调度 → 执行）中的“执行”入口即在此处，后续将补充
 * 超时控制、重试、熔断（Resilience4j）与舱壁隔离（week3）。
 */
@Slf4j
@Component
public class PluginExecutor {

    private final ObservationRegistry observationRegistry;

    /** 注入 Micrometer 观测注册中心（由 Spring Boot 自动配置提供）。 */
    public PluginExecutor(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    /**
     * 执行插件任务。
     *
     * @param plugin  目标插件
     * @param request 统一入参
     * @param context 插件运行时上下文（含 ObservationRegistry 埋点钩子）
     * @return 统一出参
     */
    public PluginResult<?> execute(AppPlugin plugin, PluginRequest request, PluginContext context) {
        String pluginId = plugin.getMetadata().id();
        Observation observation = Observation.createNotStarted("plugin.execute", observationRegistry)
                .contextualName("plugin-" + pluginId)
                .lowCardinalityKeyValue("plugin.id", pluginId)
                .lowCardinalityKeyValue("plugin.type", plugin.getMetadata().type().name());

        return observation.observe(() -> {
            log.info("[PluginExecutor] 开始执行插件 pluginId={} taskType={}", pluginId, request.taskType());
            PluginResult<?> result = plugin.execute(request);
            observation.highCardinalityKeyValue("plugin.success", String.valueOf(result.success()));
            if (!result.success()) {
                observation.highCardinalityKeyValue("plugin.error", result.errorCode());
            }
            return result;
        });
    }
}
