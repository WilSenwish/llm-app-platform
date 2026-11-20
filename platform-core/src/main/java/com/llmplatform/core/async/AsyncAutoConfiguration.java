package com.llmplatform.core.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 异步任务自动配置。
 * <p>选择逻辑：有 RabbitMQ AmqpTemplate → RabbitMQAsyncTaskProducer，否则 → InMemoryAsyncTaskProducer。
 */
@Configuration
public class AsyncAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AsyncTaskHandlerRegistry.class)
    public AsyncTaskHandlerRegistry asyncTaskHandlerRegistry(List<AsyncTaskHandler> handlers) {
        return new AsyncTaskHandlerRegistry(handlers);
    }

    @Bean
    @ConditionalOnMissingBean({AsyncTaskProducer.class, AmqpTemplate.class})
    public AsyncTaskProducer inMemoryAsyncTaskProducer(AsyncTaskHandlerRegistry registry,
                                                       ObjectProvider<ObjectMapper> mapper) {
        return new InMemoryAsyncTaskProducer(registry,
                java.util.concurrent.Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2),
                mapper.getIfAvailable(ObjectMapper::new));
    }

    @Configuration
    @ConditionalOnClass(name = "org.springframework.amqp.core.AmqpTemplate")
    static class RabbitMQSubConfig {

        @Bean
        @ConditionalOnBean({AmqpTemplate.class, AsyncTaskHandlerRegistry.class})
        @ConditionalOnMissingBean(AsyncTaskProducer.class)
        public AsyncTaskProducer rabbitMQAsyncTaskProducer(AmqpTemplate amqp,
                                                           AsyncTaskHandlerRegistry registry,
                                                           ObjectProvider<ObjectMapper> mapper) {
            return new RabbitMQAsyncTaskProducer(amqp, mapper.getIfAvailable(ObjectMapper::new));
        }
    }
}
