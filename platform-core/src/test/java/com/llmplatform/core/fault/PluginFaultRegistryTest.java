package com.llmplatform.core.fault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PluginFaultRegistryTest {

    @Test
    void createsIndependentCircuitBreakersPerPlugin() {
        PluginFaultProperties properties = new PluginFaultProperties();
        properties.getDefaults().setFailureRateThreshold(30f);
        properties.getDefaults().setSlidingWindowSize(10);
        PluginFaultRegistry registry = new PluginFaultRegistry(properties);

        var cb1 = registry.getOrCreateCircuitBreaker("plugin-a");
        var cb2 = registry.getOrCreateCircuitBreaker("plugin-b");
        var cb1Again = registry.getOrCreateCircuitBreaker("plugin-a");

        assertEquals("plugin-a", cb1.getName());
        assertEquals("plugin-b", cb2.getName());
        assertSame(cb1, cb1Again, "同插件应复用同一 CircuitBreaker 实例");
    }

    @Test
    void createsIndependentBulkheadsPerPlugin() {
        PluginFaultProperties properties = new PluginFaultProperties();
        properties.getDefaults().setMaxConcurrent(5);
        PluginFaultRegistry registry = new PluginFaultRegistry(properties);

        var bh1 = registry.getOrCreateBulkhead("plugin-a");
        var bh2 = registry.getOrCreateBulkhead("plugin-b");

        assertNotNull(bh1);
        assertNotNull(bh2);
    }

    @Test
    void pluginSpecificConfigOverridesDefaults() {
        PluginFaultProperties properties = new PluginFaultProperties();
        properties.getDefaults().setFailureRateThreshold(50f);
        properties.getDefaults().setSlidingWindowSize(20);

        PluginFaultProperties.FaultConfig ragCfg = new PluginFaultProperties.FaultConfig();
        ragCfg.setFailureRateThreshold(20f);
        ragCfg.setSlidingWindowSize(5);
        properties.getPlugins().put("plugin-rag", ragCfg);

        PluginFaultRegistry registry = new PluginFaultRegistry(properties);

        // rag 插件应该用覆盖后的配置
        var ragCb = registry.getOrCreateCircuitBreaker("plugin-rag");
        // 其他插件应该用默认配置
        var otherCb = registry.getOrCreateCircuitBreaker("plugin-other");

        // 验证配置不同：rag 的 slidingWindowSize 应该是 5
        var ragConfig = ragCb.getCircuitBreakerConfig();
        var otherConfig = otherCb.getCircuitBreakerConfig();

        assertEquals(5, ragConfig.getSlidingWindowSize());
        assertEquals(20, otherConfig.getSlidingWindowSize());
    }

    @Test
    void resolveMergesPartialOverrides() {
        PluginFaultProperties properties = new PluginFaultProperties();
        properties.getDefaults().setFailureRateThreshold(50f);
        properties.getDefaults().setSlidingWindowSize(20);
        properties.getDefaults().setMaxConcurrent(10);

        // 只覆盖 failureRateThreshold，其他回退默认
        PluginFaultProperties.FaultConfig partial = new PluginFaultProperties.FaultConfig();
        partial.setFailureRateThreshold(25f);
        properties.getPlugins().put("plugin-x", partial);

        PluginFaultProperties.FaultConfig resolved = properties.resolve("plugin-x");

        assertEquals(25f, resolved.getFailureRateThreshold());
        assertEquals(20, resolved.getSlidingWindowSize());
        assertEquals(10, resolved.getMaxConcurrent());
    }

    @Test
    void resetCircuitBreakerBackToClosed() {
        PluginFaultProperties properties = new PluginFaultProperties();
        PluginFaultRegistry registry = new PluginFaultRegistry(properties);

        var cb = registry.getOrCreateCircuitBreaker("p1");
        cb.transitionToOpenState();

        registry.reset("p1");

        var resetCb = registry.getOrCreateCircuitBreaker("p1");
        assertEquals("CLOSED", resetCb.getState().name());
    }
}
