package com.llmplatform.core.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

/**
 * 进程内异步任务生产/消费实现。
 * <p>使用 JVM 线程池（默认 CPU 核心数 × 2）异步执行 handler。
 * 适合开发/测试/单实例部署。多实例生产环境应切换 RabbitMQ 实现。
 */
@Slf4j
public class InMemoryAsyncTaskProducer implements AsyncTaskProducer {

    private final AsyncTaskHandlerRegistry registry;
    private final ExecutorService executor;
    private final ObjectMapper mapper;

    public InMemoryAsyncTaskProducer(AsyncTaskHandlerRegistry registry) {
        this(registry, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2), new ObjectMapper());
    }

    public InMemoryAsyncTaskProducer(AsyncTaskHandlerRegistry registry, ExecutorService executor, ObjectMapper mapper) {
        this.registry = registry;
        this.executor = executor;
        this.mapper = mapper;
    }

    @Override
    public String submit(String taskType, String payload) {
        String taskId = UUID.randomUUID().toString();
        AsyncTaskHandler handler = registry.find(taskType);
        if (handler == null) {
            log.warn("[AsyncProducer] 未找到 handler taskType={} taskId={} 静默丢弃", taskType, taskId);
            return taskId;
        }
        executor.submit(() -> {
            try {
                handler.handle(taskId, payload);
                log.info("[AsyncProducer] 任务完成 taskId={} taskType={}", taskId, taskType);
            } catch (Exception e) {
                log.error("[AsyncProducer] 任务执行失败 taskId={} taskType={}", taskId, taskType, e);
            }
        });
        return taskId;
    }
}
