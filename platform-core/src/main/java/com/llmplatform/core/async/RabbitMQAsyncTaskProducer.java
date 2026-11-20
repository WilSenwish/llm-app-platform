package com.llmplatform.core.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import org.springframework.amqp.core.AmqpTemplate;

/**
 * RabbitMQ 异步任务生产者。
 * <p>将任务序列化为 JSON 投递到 direct exchange，由 {@link RabbitMQTaskConsumer} 消费。
 * 仅当 classpath 有 spring-rabbit + 有 RabbitMQ 连接时生效。
 */
public class RabbitMQAsyncTaskProducer implements AsyncTaskProducer {

    static final String EXCHANGE = "llm.async.exchange";
    static final String ROUTING_KEY = "llm.async.tasks";

    private final AmqpTemplate amqp;
    private final ObjectMapper mapper;
    private final LongSupplier clockMs;

    public RabbitMQAsyncTaskProducer(AmqpTemplate amqp, ObjectMapper mapper) {
        this.amqp = amqp;
        this.mapper = mapper;
        this.clockMs = System::currentTimeMillis;
    }

    @Override
    public String submit(String taskType, String payload) {
        String taskId = UUID.randomUUID().toString();
        TaskEnvelope envelope = new TaskEnvelope(taskId, taskType, payload, clockMs.getAsLong(), 0);
        try {
            amqp.convertAndSend(EXCHANGE, ROUTING_KEY, envelope, msg -> {
                msg.getMessageProperties().setMessageId(taskId);
                msg.getMessageProperties().setHeader("taskType", taskType);
                msg.getMessageProperties().setHeader("attempt", 0);
                return msg;
            });
            return taskId;
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ 投递失败 taskType=" + taskType, e);
        }
    }

    /** 任务信封（序列化/反序列化载体）。 */
    public record TaskEnvelope(String taskId, String taskType, String payload, long createdAtMs, int attempt) {
        public static TaskEnvelope fromMap(Map<String, Object> map) {
            return new TaskEnvelope(
                    (String) map.get("taskId"),
                    (String) map.get("taskType"),
                    (String) map.get("payload"),
                    map.get("createdAtMs") != null ? ((Number) map.get("createdAtMs")).longValue() : 0,
                    map.get("attempt") != null ? ((Number) map.get("attempt")).intValue() : 0);
        }
    }
}
