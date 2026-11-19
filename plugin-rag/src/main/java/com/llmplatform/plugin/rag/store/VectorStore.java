package com.llmplatform.plugin.rag.store;

import java.util.List;

/**
 * RAG 向量存储抽象。
 * <p>底层实现可选：进程内（单实例）、Redis Set（多实例轻量）、Milvus（生产海量）。
 */
public interface VectorStore {

    /**
     * 用同一文档的新分块替换旧分块。docId 下的旧块会先清理。
     */
    void replaceDocument(String docId, List<StoredChunk> documentChunks);

    /**
     * 按余弦相似度取 topK，分数从高到低。
     *
     * @param query query embedding
     * @param topK  取前多少条
     */
    List<ScoredChunk> search(float[] query, int topK);
}
