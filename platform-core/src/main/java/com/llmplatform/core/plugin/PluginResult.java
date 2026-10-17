package com.llmplatform.core.plugin;

/**
 * 统一的插件出参契约。
 * <p>success=false 时通过 errorCode / errorMessage 透出失败原因，由内核统一处理重试与降级。
 *
 * @param <T> 业务结果类型
 */
public record PluginResult<T>(
        boolean success,
        T data,
        String errorCode,
        String errorMessage) {

    /** 构建成功结果。 */
    public static <T> PluginResult<T> success(T data) {
        return new PluginResult<>(true, data, null, null);
    }

    /** 构建失败结果。 */
    public static <T> PluginResult<T> failure(String errorCode, String errorMessage) {
        return new PluginResult<>(false, null, errorCode, errorMessage);
    }
}
