package com.llmplatform.core.model;

/**
 * 一次模型调用的入参。网关用 userId 做配额，用 traceId 串日志。
 */
public record ModelRequest(String traceId, String userId, String prompt) {
}
