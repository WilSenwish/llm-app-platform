package com.llmplatform.core.plugin;

import com.llmplatform.core.model.ModelGateway;
import io.micrometer.observation.ObservationRegistry;
import lombok.Getter;

/**
 * 默认插件上下文实现。
 * <p>供本地调试与单元测试场景使用；生产环境由网关注入携带模型网关与 MCP 注册中心的上下文。
 */
@Getter
public class DefaultPluginContext implements PluginContext {

    private final String traceId;
    private final String userId;
    private final ObservationRegistry observationRegistry;
    private final ModelGateway modelGateway;

    public DefaultPluginContext(String traceId, String userId, ObservationRegistry observationRegistry) {
        this(traceId, userId, observationRegistry, null);
    }

    public DefaultPluginContext(
            String traceId,
            String userId,
            ObservationRegistry observationRegistry,
            ModelGateway modelGateway) {
        this.traceId = traceId;
        this.userId = userId;
        this.observationRegistry = observationRegistry;
        this.modelGateway = modelGateway;
    }
}
