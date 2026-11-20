package com.llmplatform.core.async;

/**
 * 异步任务处理器接口。
 * <p>实现负责按 taskType 消费具体业务逻辑。每个任务类型一个 handler，
 * 通过 {@link AsyncTaskHandlerRegistry} 注册和查找。
 */
public interface AsyncTaskHandler {

    /** 处理的任务类型（与 {@link AsyncTaskProducer#submit} 中的 taskType 一致）。 */
    String taskType();

    /**
     * 执行任务。
     *
     * @param taskId  任务唯一 ID
     * @param payload 任务参数（JSON）
     */
    void handle(String taskId, String payload);
}
