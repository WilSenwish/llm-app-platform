package com.llmplatform.plugin.rag.store;

/**
 * 一次召回命中。score 为查询向量与块向量的余弦相似度。
 */
public record ScoredChunk(String docId, int index, String text, double score) {
}
