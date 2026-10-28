package com.llmplatform.gateway.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 端点健康标记。缺省视为可用，调用失败后标停，探测成功再恢复。
 */
final class ModelHealth {

    private final ConcurrentMap<String, Boolean> up = new ConcurrentHashMap<>();

    boolean allow(String modelId) {
        return up.getOrDefault(modelId, Boolean.TRUE);
    }

    void markDown(String modelId) {
        up.put(modelId, Boolean.FALSE);
    }

    void markUp(String modelId) {
        up.put(modelId, Boolean.TRUE);
    }
}
