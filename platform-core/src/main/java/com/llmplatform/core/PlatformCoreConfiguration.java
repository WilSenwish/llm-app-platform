package com.llmplatform.core;

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
}
