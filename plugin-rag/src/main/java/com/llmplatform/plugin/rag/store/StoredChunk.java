package com.llmplatform.plugin.rag.store;

/**
 * 已入库的一个文本块及其向量。
 */
public record StoredChunk(String docId, int index, String text, float[] vector) {
}
