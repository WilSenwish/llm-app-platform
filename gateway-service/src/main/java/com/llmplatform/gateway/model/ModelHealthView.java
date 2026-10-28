package com.llmplatform.gateway.model;

/**
 * 健康探测结果，给管理接口用。
 */
public record ModelHealthView(String modelId, boolean up) {
}
