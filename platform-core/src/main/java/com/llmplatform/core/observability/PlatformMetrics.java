package com.llmplatform.core.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * 平台级指标注册中心。
 * <p>为熔断器事件、舱壁事件、插件执行、网关限流等场景提供统一的 Counter/Timer 懒注册。
 * 底层 {@link MeterRegistry} 由 Spring Boot Actuator 自动配置提供。
 */
@Slf4j
public class PlatformMetrics {

    private final MeterRegistry registry;

    public PlatformMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // ===================== 熔断器事件 =====================

    /** 熔断器打开次数（状态 CLOSED → OPEN 转换触发）。 */
    public void circuitBreakerOpened(String pluginId, String fromState, String toState) {
        Counter.builder("llm.plugin.circuitbreaker.transitions")
                .tag("plugin.id", pluginId)
                .tag("from", fromState)
                .tag("to", toState)
                .description("CircuitBreaker 状态转换次数")
                .register(registry)
                .increment();
    }

    /** 熔断器拒绝请求次数（快速失败，请求未到达插件）。 */
    public void circuitBreakerCallNotPermitted(String pluginId) {
        Counter.builder("llm.plugin.circuitbreaker.rejected")
                .tag("plugin.id", pluginId)
                .description("CircuitBreaker OPEN 期间拒绝的请求数")
                .register(registry)
                .increment();
    }

    /** 熔断失败率超限事件。 */
    public void circuitBreakerFailureRateExceeded(String pluginId, double failureRate) {
        Counter.builder("llm.plugin.circuitbreaker.failure_rate_exceeded")
                .tag("plugin.id", pluginId)
                .description("CircuitBreaker 失败率超限次数")
                .register(registry)
                .increment();
    }

    // ===================== 舱壁事件 =====================

    /** 舱壁拒绝请求次数（并发超限快速失败）。 */
    public void bulkheadRejected(String pluginId) {
        Counter.builder("llm.plugin.bulkhead.rejected")
                .tag("plugin.id", pluginId)
                .description("Bulkhead 并发超限拒绝的请求数")
                .register(registry)
                .increment();
    }

    // ===================== 插件执行 =====================

    /** 插件执行结果（成功 / 失败 / 熔断拒绝）。 */
    public void pluginExecution(String pluginId, String taskType, String result) {
        Counter.builder("llm.plugin.execution")
                .tag("plugin.id", pluginId)
                .tag("task.type", taskType)
                .tag("result", result) // success | failed | isolated
                .description("插件执行结果计数")
                .register(registry)
                .increment();
    }

    /** 插件执行耗时。 */
    public Timer.Sample startPluginLatencySample() {
        return Timer.start(registry);
    }

    public void recordPluginLatency(String pluginId, String taskType, Timer.Sample sample) {
        sample.stop(Timer.builder("llm.plugin.latency")
                .tag("plugin.id", pluginId)
                .tag("task.type", taskType)
                .publishPercentileHistogram()
                .description("插件执行耗时分布")
                .register(registry));
    }

    // ===================== 网关限流 =====================

    /** 网关限流拒绝次数。 */
    public void rateLimitRejected(String userId) {
        Counter.builder("llm.gateway.ratelimit.rejected")
                .tag("user.id", userId == null || userId.isBlank() ? "anonymous" : userId)
                .description("网关限流拒绝次数")
                .register(registry)
                .increment();
    }
}
