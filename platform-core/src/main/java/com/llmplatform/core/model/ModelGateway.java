package com.llmplatform.core.model;

/**
 * 插件看到的模型端口。路由、超时、重试和降级都在网关实现里，插件不关心端点。
 */
public interface ModelGateway {

    /** 同步补全。全部端点失败时抛出 {@link ModelUnavailableException}。 */
    ModelCompletion complete(ModelRequest request);

    /**
     * 流式补全。首 token 之前失败会换下一个端点；已经吐出 token 后失败则直接抛出。
     */
    void stream(ModelRequest request, ModelStreamListener listener);
}
