package com.llmplatform.plugin.rag.answer;

/**
 * 一次入库的结果。
 */
public record IngestResult(String docId, int chunkCount) {
}
