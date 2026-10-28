package com.llmplatform.core.model;

/**
 * 一次非流式模型调用的结果。modelId 是实际应答的端点，降级后会变成备选端点。
 */
public record ModelCompletion(String modelId, String text) {
}
