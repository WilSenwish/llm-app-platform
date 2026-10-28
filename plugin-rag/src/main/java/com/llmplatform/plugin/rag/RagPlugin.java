package com.llmplatform.plugin.rag;

import com.llmplatform.core.model.ModelGateway;
import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;
import com.llmplatform.plugin.rag.answer.IngestResult;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG 问答插件。
 * <p>提供无参构造，供 {@code ServiceLoader} 发现；Spring 容器同样使用该构造，管道在插件内部装配。
 * 没有模型网关时回答是资料拼接；init 注入网关后，问答改为把资料交给网关生成。
 */
@Slf4j
@Component
public class RagPlugin implements AppPlugin {

    private final RagPipeline pipeline;
    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
    private ModelGateway modelGateway;

    /** SPI 与 Spring 都走这条路径，保证没有容器时也能被加载。 */
    public RagPlugin() {
        this(new RagPipeline());
    }

    /** 测试注入管道，避免每个用例都走默认存储。 */
    public RagPlugin(RagPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata(
                "plugin-rag",
                "RAG 问答插件",
                "0.0.1",
                "可插拔的检索增强生成插件，第 1 周为内存哈希检索",
                PluginType.RAG);
    }

    @Override
    public void init(PluginContext context) {
        this.observationRegistry = context.getObservationRegistry();
        this.modelGateway = context.getModelGateway();
        log.info("[RagPlugin] 初始化 traceId={} modelGateway={}", context.getTraceId(), modelGateway != null);
    }

    @Override
    public PluginResult<?> execute(PluginRequest request) {
        log.info("[RagPlugin] 收到任务 taskType={} traceId={}", request.taskType(), request.traceId());
        return switch (request.taskType()) {
            case RagTasks.INGEST -> ingest(request.payload());
            case RagTasks.QUERY -> query(request);
            default -> PluginResult.failure(RagErrorCodes.UNSUPPORTED_TASK, "不支持的任务: " + request.taskType());
        };
    }

    @Override
    public void destroy() {
        log.info("[RagPlugin] 销毁");
    }

    private PluginResult<IngestResult> ingest(Map<String, Object> payload) {
        String docId = requiredText(payload, "docId");
        String text = requiredText(payload, "text");
        if (docId == null || text == null) {
            return PluginResult.failure(RagErrorCodes.INVALID_PAYLOAD, "rag.ingest 需要 docId 与 text");
        }
        return PluginResult.success(pipeline.ingest(docId, text, observationRegistry));
    }

    private PluginResult<?> query(PluginRequest request) {
        String query = requiredText(request.payload(), "query");
        if (query == null) {
            return PluginResult.failure(RagErrorCodes.INVALID_PAYLOAD, "rag.query 需要 query");
        }
        int topK = topK(request.payload());
        if (topK < 0) {
            return PluginResult.failure(RagErrorCodes.INVALID_PAYLOAD, "topK 必须是 1 到 10 的数字");
        }
        return pipeline.query(query, topK, observationRegistry, modelGateway, request.traceId(), request.userId());
    }

    private static String requiredText(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    /** 缺省 3；非法数字返回 -1。 */
    private static int topK(Map<String, Object> payload) {
        Object raw = payload.get("topK");
        if (raw == null) {
            return RagPipeline.defaultTopK();
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return -1;
    }
}
