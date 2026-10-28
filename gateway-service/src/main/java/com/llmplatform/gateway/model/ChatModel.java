package com.llmplatform.gateway.model;

import java.util.function.Consumer;

/**
 * 一个可调用的模型端点。路由层只依赖这个接口。
 */
public interface ChatModel {

    String id();

    /** 同步补全。失败抛出 {@link com.llmplatform.core.model.ModelUnavailableException}。 */
    String complete(String prompt);

    /** 流式补全。在第一个增量之前失败才允许路由层换端点。 */
    void stream(String prompt, Consumer<String> onDelta);

    /** 主动探测。失败抛出运行时异常，路由层会把端点标成不可用。 */
    void probe();
}
