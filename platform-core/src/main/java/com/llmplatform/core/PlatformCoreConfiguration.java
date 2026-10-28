package com.llmplatform.core;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 平台内核自动装配。
 * <p>任何引入 platform-core 的模块（如 gateway-service）导入本配置即可扫描并注册内核 Bean
 * （插件注册中心、插件执行器等）。
 */
@Configuration
@ComponentScan("com.llmplatform.core")
public class PlatformCoreConfiguration {

    /**
     * 没有接入 Actuator / OTEL 时提供进程内观测注册中心。
     * 第 4 周观测栈会提供自己的 {@link ObservationRegistry}，本 Bean 自动让路。
     */
    @Bean
    @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}
