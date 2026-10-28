package com.llmplatform.plugin.rag.answer;

import java.util.List;

/**
 * 一次检索的回答。modelId 为空表示没有走生成模型，answer 是资料拼接。
 */
public record QueryResult(String answer, List<QueryHit> hits, String modelId) {

    public QueryResult(String answer, List<QueryHit> hits) {
        this(answer, hits, null);
    }
}
