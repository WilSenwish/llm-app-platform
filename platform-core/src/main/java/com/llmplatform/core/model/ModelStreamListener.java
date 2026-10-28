package com.llmplatform.core.model;

/**
 * 流式增量。第一个 token 发出之后，网关不再切换端点。
 */
public interface ModelStreamListener {

    void onDelta(String modelId, String delta);

    void onComplete(String modelId);
}
