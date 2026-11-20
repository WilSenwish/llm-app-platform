package com.llmplatform.core.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;

/**
 * RabbitMQ 任务队列基础设施配置。
 * <p>声明 Exchange / Queue / Binding，并注册 @RabbitListener 消费器。
 * 仅当 classpath 有 spring-rabbit 且上下文有 AsyncTaskHandlerRegistry 时生效。
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.core.AmqpTemplate")
@ConditionalOnBean(AsyncTaskHandlerRegistry.class)
public class RabbitMQQueueConfiguration {

    public static final String QUEUE_NAME = "llm.async.tasks";

    @Bean
    public DirectExchange llmAsyncExchange() {
        return new DirectExchange(RabbitMQAsyncTaskProducer.EXCHANGE, true, false);
    }

    @Bean
    public Queue llmAsyncQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding llmAsyncBinding() {
        return BindingBuilder.bind(llmAsyncQueue())
                .to(llmAsyncExchange())
                .with(RabbitMQAsyncTaskProducer.ROUTING_KEY);
    }

    @Bean
    public RabbitMQTaskConsumer rabbitMQTaskConsumer(AsyncTaskHandlerRegistry registry) {
        return new RabbitMQTaskConsumer(registry);
    }

    @Slf4j
    static class RabbitMQTaskConsumer {

        private final AsyncTaskHandlerRegistry registry;

        RabbitMQTaskConsumer(AsyncTaskHandlerRegistry registry) {
            this.registry = registry;
        }

        @RabbitListener(queues = QUEUE_NAME)
        public void consume(RabbitMQAsyncTaskProducer.TaskEnvelope envelope,
                            @Header(value = AmqpHeaders.REDELIVERED, required = false) Boolean redelivered) {
            AsyncTaskHandler handler = registry.find(envelope.taskType());
            if (handler == null) {
                log.warn("[RabbitMQ] 未找到 handler taskType={} taskId={} 丢弃",
                        envelope.taskType(), envelope.taskId());
                return;
            }
            try {
                handler.handle(envelope.taskId(), envelope.payload());
                log.info("[RabbitMQ] 任务完成 taskId={} taskType={} attempt={}",
                        envelope.taskId(), envelope.taskType(), envelope.attempt());
            } catch (Exception e) {
                log.error("[RabbitMQ] 任务失败 taskId={} taskType={} attempt={}",
                        envelope.taskId(), envelope.taskType(), envelope.attempt(), e);
                if (envelope.attempt() >= 3) {
                    log.warn("[RabbitMQ] 任务进入死信 taskId={} taskType={}",
                            envelope.taskId(), envelope.taskType());
                }
                throw new RuntimeException(e); // 让 RabbitMQ 按配置重试
            }
        }
    }
}
