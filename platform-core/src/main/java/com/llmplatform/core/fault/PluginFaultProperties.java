package com.llmplatform.core.fault;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件级熔断 & 舱壁隔离配置。
 * <p>支持全局默认值 + 按插件 ID 覆盖，满足"RAG 故障不影响 CodeAgent"的故障隔离需求。
 * <p>示例 yml（gateway-service）:
 * <pre>
 * llm:
 *   platform:
 *     fault:
 *       defaults:
 *         failure-rate-threshold: 50
 *         wait-duration-open: 10s
 *         max-concurrent: 10
 *       plugins:
 *         plugin-rag:
 *           failure-rate-threshold: 30
 *           max-concurrent: 5
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "llm.platform.fault")
public class PluginFaultProperties {

    /** 全局默认配置，未在 plugins 中显式声明的插件走这里。 */
    private FaultConfig defaults = new FaultConfig();

    /** 按插件 ID 覆盖。key = 插件 ID（如 plugin-rag）。 */
    private Map<String, FaultConfig> plugins = new HashMap<>();

    /**
     * 解析指定插件的有效配置：优先取 {@link #plugins} 中的覆盖项，缺省回退到 {@link #defaults}。
     */
    public FaultConfig resolve(String pluginId) {
        FaultConfig specific = plugins.get(pluginId);
        if (specific == null) {
            return defaults;
        }
        // 合并：specific 非空字段覆盖 defaults
        FaultConfig merged = defaults.copy();
        if (specific.getFailureRateThreshold() != null) {
            merged.setFailureRateThreshold(specific.getFailureRateThreshold());
        }
        if (specific.getWaitDurationOpen() != null) {
            merged.setWaitDurationOpen(specific.getWaitDurationOpen());
        }
        if (specific.getPermittedNumberHalfOpen() != null) {
            merged.setPermittedNumberHalfOpen(specific.getPermittedNumberHalfOpen());
        }
        if (specific.getSlidingWindowSize() != null) {
            merged.setSlidingWindowSize(specific.getSlidingWindowSize());
        }
        if (specific.getMinimumNumberOfCalls() != null) {
            merged.setMinimumNumberOfCalls(specific.getMinimumNumberOfCalls());
        }
        if (specific.getMaxConcurrent() != null) {
            merged.setMaxConcurrent(specific.getMaxConcurrent());
        }
        return merged;
    }

    /** 单个插件的熔断 + 舱壁配置。 */
    @Data
    public static class FaultConfig {

        /** 失败率阈值（百分比），达到后 CircuitBreaker 打开。默认 50。 */
        private Float failureRateThreshold = 50f;

        /** 打开态持续时长，到期后进入 half-open 尝试放行。默认 10s。 */
        private Duration waitDurationOpen = Duration.ofSeconds(10);

        /** half-open 态允许通过的请求数。默认 10。 */
        private Integer permittedNumberHalfOpen = 10;

        /** 滑动窗口大小（最近 N 次调用）。默认 20。 */
        private Integer slidingWindowSize = 20;

        /** CB 开始评估失败率所需的最小调用数。默认 5，避免窗口未填满时 failureRate=-1。 */
        private Integer minimumNumberOfCalls = 5;

        /** 舱壁最大并发数，超过后新请求快速失败。默认 10。 */
        private Integer maxConcurrent = 10;

        public FaultConfig copy() {
            FaultConfig c = new FaultConfig();
            c.failureRateThreshold = this.failureRateThreshold;
            c.waitDurationOpen = this.waitDurationOpen;
            c.permittedNumberHalfOpen = this.permittedNumberHalfOpen;
            c.slidingWindowSize = this.slidingWindowSize;
            c.minimumNumberOfCalls = this.minimumNumberOfCalls;
            c.maxConcurrent = this.maxConcurrent;
            return c;
        }
    }
}
