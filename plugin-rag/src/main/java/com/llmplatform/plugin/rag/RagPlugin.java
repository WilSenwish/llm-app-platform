package com.llmplatform.plugin.rag;

import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG 问答插件（骨架）。
 * <p>作为可插拔插件被平台内核加载，用于验证 SPI 插件体系：注册中心注册 → 前置编排执行 → 埋点上报。
 * <p>week1 将接入向量库（Milvus）、Embedding、召回、重排与 LLM 合成，补全真实检索链路。
 */
@Slf4j
@Component
public class RagPlugin implements AppPlugin {

    private PluginContext context;

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata(
                "plugin-rag",
                "RAG 问答插件",
                "0.0.1",
                "可插拔的检索增强生成插件",
                PluginType.RAG);
    }

    @Override
    public void init(PluginContext context) {
        this.context = context;
        log.info("[RagPlugin] 初始化 traceId={}", context.getTraceId());
    }

    @Override
    public PluginResult<?> execute(PluginRequest request) {
        log.info("[RagPlugin] 收到任务 taskType={} traceId={}", request.taskType(), request.traceId());
        // TODO(week1): 文档入库 / 向量召回 / 重排 / LLM 合成
        return PluginResult.success("RAG 骨架占位响应，待 week1 接入检索链路");
    }

    @Override
    public void destroy() {
        log.info("[RagPlugin] 销毁");
    }
}
