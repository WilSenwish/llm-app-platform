package com.llmplatform.core.fault;

import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginResult;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 插件级故障隔离切面。
 * <p>拦截 {@code PluginExecutor.execute()}，为每个插件独立包裹 CircuitBreaker（熔断）+ Bulkhead（舱壁）。
 * <p>当插件持续失败触发熔断打开时，后续请求快速返回失败，不拖垮线程也不影响其他插件。
 * 当某插件并发超过舱壁阈值时，多余请求也快速失败。
 * <p>熔断判定规则（两条路径任一均记为失败）:
 * <ol>
 *   <li>{@link AppPlugin#execute} 抛出 RuntimeException → CB.recordError</li>
 *   <li>{@link PluginResult#success()} == false → CB.recordError</li>
 * </ol>
 */
@Slf4j
@Aspect
public class FaultIsolationAspect {

    private final PluginFaultRegistry registry;

    public FaultIsolationAspect(PluginFaultRegistry registry) {
        this.registry = registry;
    }

    /**
     * 包裹 PluginExecutor.execute(AppPlugin, PluginRequest, PluginContext)。
     * <p>执行链路：CircuitBreaker（熔断判定） → Bulkhead（控制并发） → 原始执行。
     */
    @Around("execution(* com.llmplatform.core.executor.PluginExecutor.execute(..))")
    public Object aroundPluginExecute(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        AppPlugin plugin = (AppPlugin) args[0];
        String pluginId = plugin.getMetadata().id();

        CircuitBreaker cb = registry.getOrCreateCircuitBreaker(pluginId);
        Bulkhead bulkhead = registry.getOrCreateBulkhead(pluginId);

        // 原始执行（pjp.proceed() 内部已 catch 异常转 PluginResult.failure）
        Supplier<PluginResult<?>> rawSupplier = () -> {
            try {
                return (PluginResult<?>) pjp.proceed();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };

        // 整条装饰链：
        // CircuitBreaker 判定 → Bulkhead 控制并发 → rawSupplier
        Supplier<PluginResult<?>> decorated = () -> {
            // 1. CircuitBreaker 获取权限（打开态直接抛 CallNotPermittedException）
            cb.acquirePermission();

            long startNanos = System.nanoTime();
            try {
                // 2. Bulkhead 控制并发
                PluginResult<?> result = bulkhead.executeSupplier(rawSupplier);
                long duration = System.nanoTime() - startNanos;

                // 3. 根据 PluginResult.success 判定成功/失败
                if (result != null && result.success()) {
                    cb.onSuccess(duration, TimeUnit.NANOSECONDS);
                } else {
                    cb.onError(duration, TimeUnit.NANOSECONDS, null);
                }
                return result;
            } catch (BulkheadFullException e) {
                // 舱壁满也记为 CB 错误，让熔断统计涵盖并发耗尽场景
                long duration = System.nanoTime() - startNanos;
                cb.onError(duration, TimeUnit.NANOSECONDS, e);
                throw e; // 透传到外层 catch
            } catch (Throwable t) {
                // 原始执行抛异常（虽然 PluginExecutor 已 catch 大部分）
                long duration = System.nanoTime() - startNanos;
                cb.onError(duration, TimeUnit.NANOSECONDS, t);
                throw new RuntimeException(t);
            }
        };

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            log.warn("[FaultIsolation] CircuitBreaker OPEN pluginId={}", pluginId);
            return PluginResult.failure(
                    PlatformErrorCodes.FAULT_ISOLATED, "插件熔断，快速失败: " + pluginId);
        } catch (BulkheadFullException e) {
            log.warn("[FaultIsolation] Bulkhead FULL pluginId={}", pluginId);
            return PluginResult.failure(
                    PlatformErrorCodes.FAULT_ISOLATED, "插件舱壁已满，快速失败: " + pluginId);
        }
    }
}
