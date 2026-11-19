package com.llmplatform.core.fault;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 插件级 CircuitBreaker + Bulkhead 实例注册中心。
 * <p>每个插件有独立的熔断/舱壁实例，RAG 故障不会波及 CodeAgent 等其他插件。
 * <p>实例在首次访问时按配置懒创建，后续请求复用同一实例。
 */
@Slf4j
public class PluginFaultRegistry {

    private final PluginFaultProperties properties;
    private final ConcurrentMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

    public PluginFaultRegistry(PluginFaultProperties properties) {
        this.properties = properties;
    }

    /** 获取或创建指定插件的 CircuitBreaker（线程安全懒创建）。 */
    public CircuitBreaker getOrCreateCircuitBreaker(String pluginId) {
        return circuitBreakers.computeIfAbsent(pluginId, id -> {
            PluginFaultProperties.FaultConfig cfg = properties.resolve(id);
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(cfg.getFailureRateThreshold())
                    .waitDurationInOpenState(cfg.getWaitDurationOpen() != null
                            ? cfg.getWaitDurationOpen()
                            : Duration.ofSeconds(10))
                    .permittedNumberOfCallsInHalfOpenState(cfg.getPermittedNumberHalfOpen())
                    .slidingWindowSize(cfg.getSlidingWindowSize())
                    .minimumNumberOfCalls(cfg.getMinimumNumberOfCalls())
                    .build();
            CircuitBreaker cb = CircuitBreaker.of(id, cbConfig);
            log.info("[FaultRegistry] 创建 CircuitBreaker pluginId={} threshold={}% window={}",
                    id, cfg.getFailureRateThreshold(), cfg.getSlidingWindowSize());
            return cb;
        });
    }

    /** 获取或创建指定插件的 Bulkhead（线程安全懒创建）。 */
    public Bulkhead getOrCreateBulkhead(String pluginId) {
        return bulkheads.computeIfAbsent(pluginId, id -> {
            PluginFaultProperties.FaultConfig cfg = properties.resolve(id);
            BulkheadConfig bhConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(cfg.getMaxConcurrent())
                    .build();
            Bulkhead bh = Bulkhead.of(id, bhConfig);
            log.info("[FaultRegistry] 创建 Bulkhead pluginId={} maxConcurrent={}",
                    id, cfg.getMaxConcurrent());
            return bh;
        });
    }

    /** 重置指定插件的熔断/舱壁状态（测试用 + 运维手动恢复）。 */
    public void reset(String pluginId) {
        CircuitBreaker cb = circuitBreakers.get(pluginId);
        if (cb != null) {
            cb.reset();
        }
        // Bulkhead 没有 reset 方法，直接移除让下次访问时重建
        bulkheads.remove(pluginId);
        log.info("[FaultRegistry] 重置 pluginId={}", pluginId);
    }
}
