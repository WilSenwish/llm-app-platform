package com.llmplatform.core.orchestration;

import com.llmplatform.core.plugin.PluginResult;

/**
 * 前置编排结果。accepted 时 {@code plan} 非空；拒绝时 {@code error} 非空。
 */
public record PreparationOutcome(ExecutionPlan plan, PluginResult<?> error) {

    public static PreparationOutcome ready(ExecutionPlan plan) {
        return new PreparationOutcome(plan, null);
    }

    public static PreparationOutcome rejected(PluginResult<?> error) {
        return new PreparationOutcome(null, error);
    }

    public boolean accepted() {
        return plan != null;
    }
}
