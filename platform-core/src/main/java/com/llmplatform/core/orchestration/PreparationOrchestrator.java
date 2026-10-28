package com.llmplatform.core.orchestration;

import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 三层执行器的第一层：前置编排。
 * <p>只做校验和路由，不碰插件实例。调度与执行交给后续两层。
 */
@Slf4j
@Component
public class PreparationOrchestrator {

    private final ObservationRegistry observationRegistry;
    private final TaskRouteTable routeTable;

    public PreparationOrchestrator(ObservationRegistry observationRegistry, TaskRouteTable routeTable) {
        this.observationRegistry = observationRegistry;
        this.routeTable = routeTable;
    }

    /** 把请求整理成执行计划；校验失败时返回拒绝结果，不抛异常。 */
    public PreparationOutcome prepare(PluginRequest request) {
        Observation observation = Observation.createNotStarted("orchestration.prepare", observationRegistry);
        if (request != null && request.taskType() != null) {
            observation.highCardinalityKeyValue("task.type", request.taskType());
        }
        if (isValid(request)) {
            routeTable.resolve(request).ifPresent(hit -> tagRoute(observation, hit));
        }
        return observation.observe(() -> doPrepare(request));
    }

    /**
     * 前缀路由的插件 ID 集合有限，可以做低基数标签。
     * 调用方传入的 pluginId 不做低基数，避免指标维度被请求打爆。
     */
    private static void tagRoute(Observation observation, TaskRouteTable.RouteHit hit) {
        if ("task-prefix".equals(hit.reason())) {
            observation.lowCardinalityKeyValue("plugin.id", hit.pluginId());
            return;
        }
        observation.lowCardinalityKeyValue("plugin.id", "explicit");
        observation.highCardinalityKeyValue("plugin.requested_id", hit.pluginId());
    }

    private PreparationOutcome doPrepare(PluginRequest request) {
        if (!isValid(request)) {
            return PreparationOutcome.rejected(
                    PluginResult.failure(PlatformErrorCodes.INVALID_REQUEST, "请求缺少 traceId、userId、taskType 或 payload"));
        }
        return routeTable.resolve(request)
                .map(hit -> {
                    log.info("[Preparation] route pluginId={} reason={} taskType={}",
                            hit.pluginId(), hit.reason(), request.taskType());
                    return PreparationOutcome.ready(new ExecutionPlan(hit.pluginId(), request, hit.reason()));
                })
                .orElseGet(() -> PreparationOutcome.rejected(PluginResult.failure(
                        PlatformErrorCodes.ROUTE_NOT_FOUND,
                        "没有插件能处理 taskType=" + request.taskType())));
    }

    private static boolean isValid(PluginRequest request) {
        return request != null
                && notBlank(request.traceId())
                && notBlank(request.userId())
                && notBlank(request.taskType())
                && request.payload() != null;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
