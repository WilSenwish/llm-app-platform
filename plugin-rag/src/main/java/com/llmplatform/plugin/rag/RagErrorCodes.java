package com.llmplatform.plugin.rag;

/**
 * RAG 插件自己的错误码。与内核错误码分开，避免平台内核认识检索细节。
 */
public final class RagErrorCodes {

    /** 入库或提问缺少必要字段。 */
    public static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";

    /** 插件不认识该 taskType。 */
    public static final String UNSUPPORTED_TASK = "UNSUPPORTED_TASK";

    /** 最高相似度低于证据阈值，拒绝编造回答。 */
    public static final String NO_EVIDENCE = "NO_EVIDENCE";

    /** 已召回到资料，但模型网关全部端点不可用。 */
    public static final String MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE";

    private RagErrorCodes() {
    }
}
