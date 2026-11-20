package com.llmplatform.core.async;

/**
 * 异步任务生产者接口。
 * <p>平台统一的"长耗时离线任务"投递入口。底层可切换：
 * 进程内（开发/测试）、RabbitMQ（生产多实例）。
 */
public interface AsyncTaskProducer {

    /**
     * 投递一个异步任务。
     *
     * @param taskType 任务类型（如 rag.rebuild-index / embeddings.batch）
     * @param payload  任务参数（JSON 字符串）
     * @return 任务唯一 ID（实现负责生成）
     */
    String submit(String taskType, String payload);

    /** 队列名称（用于 RabbitMQ 绑定 / DTO 中暴露）。 */
    default String queueName() {
        return "llm.async.tasks";
    }
}
