package com.llmplatform.core.executor;

import com.llmplatform.core.orchestration.ExecutionPlan;
import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.DefaultPluginContext;
import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginRegistry;
import com.llmplatform.core.plugin.PluginResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 三层执行器的第二层：调度。
 * <p>按执行计划从注册中心取出插件，组装本次请求的上下文，再交给执行层。
 */
@Slf4j
@Component
public class ExecutionDispatcher {

    private final PluginRegistry registry;
    private final PluginExecutor pluginExecutor;
    private final ObservationRegistry observationRegistry;

    public ExecutionDispatcher(
            PluginRegistry registry,
            PluginExecutor pluginExecutor,
            ObservationRegistry observationRegistry) {
        this.registry = registry;
        this.pluginExecutor = pluginExecutor;
        this.observationRegistry = observationRegistry;
    }

    /** 调度一次已经编排好的计划。插件不存在时返回失败结果，不抛异常。 */
    public PluginResult<?> dispatch(ExecutionPlan plan) {
        Optional<AppPlugin> plugin = registry.get(plan.pluginId());
        Observation observation = Observation.createNotStarted("orchestration.dispatch", observationRegistry)
                .lowCardinalityKeyValue("plugin.id", plugin.isPresent() ? plan.pluginId() : "unregistered");
        if (plugin.isEmpty()) {
            observation.highCardinalityKeyValue("plugin.requested_id", plan.pluginId());
        }
        return observation.observe(() -> doDispatch(plugin, plan));
    }

    private PluginResult<?> doDispatch(Optional<AppPlugin> plugin, ExecutionPlan plan) {
        if (plugin.isEmpty()) {
            log.warn("[Dispatcher] 插件未注册 pluginId={}", plan.pluginId());
            return PluginResult.failure(
                    PlatformErrorCodes.PLUGIN_NOT_FOUND, "插件未注册: " + plan.pluginId());
        }
        PluginContext context = new DefaultPluginContext(
                plan.request().traceId(), plan.request().userId(), observationRegistry);
        return pluginExecutor.execute(plugin.get(), plan.request(), context);
    }
}
