package com.llmplatform.core.model;

/**
 * 端点超时、网络失败或全部候选都不可用。调用方据此降级或向用户返回失败。
 */
public class ModelUnavailableException extends RuntimeException {

    public ModelUnavailableException(String message) {
        super(message);
    }

    public ModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
