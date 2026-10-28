package com.llmplatform.plugin.rag;

/**
 * plugin-rag 识别的任务类型。前缀 {@code rag.} 与内核路由表约定一致。
 */
public final class RagTasks {

    /** 文档入库：payload 需要 docId、text。 */
    public static final String INGEST = "rag.ingest";

    /** 检索问答：payload 需要 query，topK 可选。 */
    public static final String QUERY = "rag.query";

    private RagTasks() {
    }
}
