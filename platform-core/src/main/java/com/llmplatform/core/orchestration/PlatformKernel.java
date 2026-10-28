package com.llmplatform.core.orchestration;

import com.llmplatform.core.executor.ExecutionDispatcher;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import org.springframework.stereotype.Component;

/**
 * 平台内核入口。固定走完三层：前置编排 → 调度 → 执行。
 */
@Component
public class PlatformKernel {

    private final PreparationOrchestrator orchestrator;
    private final ExecutionDispatcher dispatcher;

    public PlatformKernel(PreparationOrchestrator orchestrator, ExecutionDispatcher dispatcher) {
        this.orchestrator = orchestrator;
        this.dispatcher = dispatcher;
    }

    /** 处理一次插件请求。编排拒绝时直接返回错误，不再进入调度。 */
    public PluginResult<?> handle(PluginRequest request) {
        PreparationOutcome outcome = orchestrator.prepare(request);
        if (!outcome.accepted()) {
            return outcome.error();
        }
        return dispatcher.dispatch(outcome.plan());
    }
}
