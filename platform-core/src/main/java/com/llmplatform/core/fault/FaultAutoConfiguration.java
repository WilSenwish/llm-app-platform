package com.llmplatform.core.fault;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 插件级熔断 & 舱壁隔离自动配置。
 * <p>引入本配置即自动注册 {@link PluginFaultProperties}、{@link PluginFaultRegistry}、
 * {@link FaultIsolationAspect}，让所有插件执行自动享受独立故障隔离。
 */
@Configuration
@EnableConfigurationProperties(PluginFaultProperties.class)
public class FaultAutoConfiguration {

    @Bean
    public PluginFaultRegistry pluginFaultRegistry(PluginFaultProperties properties) {
        return new PluginFaultRegistry(properties);
    }

    @Bean
    public FaultIsolationAspect faultIsolationAspect(PluginFaultRegistry registry) {
        return new FaultIsolationAspect(registry);
    }
}
