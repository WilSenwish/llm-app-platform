package com.llmplatform.plugin.rag.answer;

/**
 * 一条召回证据，返回给调用方。
 */
public record QueryHit(String docId, int chunkIndex, String text, double score) {
}
