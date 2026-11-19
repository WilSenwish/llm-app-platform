package com.llmplatform.plugin.rag;

import com.llmplatform.core.model.ModelCompletion;
import com.llmplatform.core.model.ModelGateway;
import com.llmplatform.core.model.ModelRequest;
import com.llmplatform.core.model.ModelUnavailableException;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.plugin.rag.answer.IngestResult;
import com.llmplatform.plugin.rag.answer.QueryHit;
import com.llmplatform.plugin.rag.answer.QueryResult;
import com.llmplatform.plugin.rag.embed.HashingEmbeddingModel;
import com.llmplatform.plugin.rag.ingest.TextChunker;
import com.llmplatform.plugin.rag.store.InMemoryVectorStore;
import com.llmplatform.plugin.rag.store.ScoredChunk;
import com.llmplatform.plugin.rag.store.StoredChunk;
import com.llmplatform.plugin.rag.store.VectorStore;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * 入库、向量化、召回。有模型网关时用资料约束生成；没有网关时把命中片段拼成回答。
 */
public final class RagPipeline {

    /** 低于该余弦相似度视为没有证据。1024 维哈希下，字面无关文本约在 0.15 以下，相关中文问句约在 0.35 以上。 */
    static final double EVIDENCE_THRESHOLD = 0.25;

    private static final int DEFAULT_TOP_K = 3;

    private final TextChunker chunker;
    private final HashingEmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public RagPipeline() {
        this(new TextChunker(), new HashingEmbeddingModel(), new InMemoryVectorStore());
    }

    public RagPipeline(TextChunker chunker, HashingEmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /** 切块、向量化并替换该文档的旧块。 */
    public IngestResult ingest(String docId, String text, ObservationRegistry observationRegistry) {
        return Observation.createNotStarted("rag.ingest", observationRegistry)
                .lowCardinalityKeyValue("plugin.id", "plugin-rag")
                .observe(() -> doIngest(docId, text));
    }

    /** 召回 topK。没有模型网关时拼接命中片段；有网关时只把资料交给模型。 */
    public PluginResult<QueryResult> query(String query, int topK, ObservationRegistry observationRegistry) {
        return query(query, topK, observationRegistry, null, null, null);
    }

    /**
     * @param modelGateway 为空则不生成
     */
    public PluginResult<QueryResult> query(
            String query,
            int topK,
            ObservationRegistry observationRegistry,
            ModelGateway modelGateway,
            String traceId,
            String userId) {
        int limited = Math.max(1, Math.min(topK, 10));
        return Observation.createNotStarted("rag.retrieve", observationRegistry)
                .lowCardinalityKeyValue("plugin.id", "plugin-rag")
                .observe(() -> doQuery(query, limited, modelGateway, traceId, userId));
    }

    private IngestResult doIngest(String docId, String text) {
        List<String> parts = chunker.split(text);
        List<StoredChunk> stored = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            stored.add(new StoredChunk(docId, i, parts.get(i), embeddingModel.embed(parts.get(i))));
        }
        vectorStore.replaceDocument(docId, stored);
        return new IngestResult(docId, stored.size());
    }

    private PluginResult<QueryResult> doQuery(
            String query, int topK, ModelGateway modelGateway, String traceId, String userId) {
        List<ScoredChunk> hits = vectorStore.search(embeddingModel.embed(query), topK);
        if (hits.isEmpty() || hits.get(0).score() < EVIDENCE_THRESHOLD) {
            return PluginResult.failure(RagErrorCodes.NO_EVIDENCE, "未检索到足够相关的资料");
        }
        List<QueryHit> evidence = hits.stream()
                .map(hit -> new QueryHit(hit.docId(), hit.index(), hit.text(), hit.score()))
                .toList();
        if (modelGateway == null) {
            return PluginResult.success(new QueryResult(compose(evidence), evidence));
        }
        try {
            ModelCompletion completion = modelGateway.complete(
                    new ModelRequest(traceId, userId, groundedPrompt(query, evidence)));
            if (completion.text() == null || completion.text().isBlank()) {
                return PluginResult.failure(RagErrorCodes.MODEL_UNAVAILABLE, "模型返回空内容");
            }
            return PluginResult.success(new QueryResult(completion.text(), evidence, completion.modelId()));
        } catch (ModelUnavailableException ex) {
            return PluginResult.failure(RagErrorCodes.MODEL_UNAVAILABLE, ex.getMessage());
        }
    }

    static String groundedPrompt(String query, List<QueryHit> hits) {
        StringBuilder evidence = new StringBuilder();
        for (QueryHit hit : hits) {
            evidence.append("- ").append(hit.text()).append('\n');
        }
        return "只根据下面的资料回答问题，资料没有的内容直接说不知道。\n资料：\n" + evidence + "问题：" + query;
    }

    static int defaultTopK() {
        return DEFAULT_TOP_K;
    }

    private static String compose(List<QueryHit> hits) {
        StringBuilder answer = new StringBuilder("根据检索到的资料：");
        for (QueryHit hit : hits) {
            answer.append("\n- ").append(hit.text());
        }
        return answer.toString();
    }
}
