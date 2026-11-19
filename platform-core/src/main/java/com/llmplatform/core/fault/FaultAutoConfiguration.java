package com.llmplatform.core.fault;

import com.llmplatform.core.observability.PlatformMetrics;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 插件级熔断 & 舱壁隔离自动配置。
 * <p>引入本配置即自动注册 {@link PluginFaultProperties}、{@link PluginFaultRegistry}、
 * {@link FaultIsolationAspect}，让所有插件执行自动享受独立故障隔离。
 * <p>{@link PlatformMetrics} 可选注入——没有 Actuator 时为 null，事件监听自动跳过。
 */
@Configuration
@EnableConfigurationProperties(PluginFaultProperties.class)
public class FaultAutoConfiguration {

    @Bean
    public PluginFaultRegistry pluginFaultRegistry(PluginFaultProperties properties,
                                                    ObjectProvider<PlatformMetrics> metricsProvider) {
        return new PluginFaultRegistry(properties, metricsProvider.getIfAvailable());
    }

    @Bean
    public FaultIsolationAspect faultIsolationAspect(PluginFaultRegistry registry) {
        return new FaultIsolationAspect(registry);
    }
}
