package com.llmplatform.core.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 平台指标自动配置。
 * <p>有 MeterRegistry（Spring Boot Actuator 自动提供）时注册 {@link PlatformMetrics}。
 * 没有 MeterRegistry 时（纯 platform-core 单测场景），相关 Counter 调用会被 Guard 吞掉，不影响业务逻辑。
 */
@Configuration
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public PlatformMetrics platformMetrics(MeterRegistry meterRegistry) {
        return new PlatformMetrics(meterRegistry);
    }
}
