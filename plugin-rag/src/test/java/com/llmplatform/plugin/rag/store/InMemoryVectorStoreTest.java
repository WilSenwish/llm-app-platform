package com.llmplatform.plugin.rag.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.plugin.rag.embed.HashingEmbeddingModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryVectorStoreTest {

    @Test
    void searchReturnsCloserDocumentFirst() {
        HashingEmbeddingModel model = new HashingEmbeddingModel();
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.replaceDocument("doc-rag", List.of(chunk("doc-rag", "平台内核通过插件注册中心加载 RAG 插件。", model)));
        store.replaceDocument("doc-other", List.of(chunk("doc-other", "今日天气晴朗适合出门散步。", model)));

        List<ScoredChunk> hits = store.search(model.embed("RAG 插件如何被加载"), 2);

        assertEquals("doc-rag", hits.get(0).docId());
        assertTrue(hits.get(0).score() > hits.get(1).score());
    }

    @Test
    void replacingDocumentDropsPreviousChunks() {
        HashingEmbeddingModel model = new HashingEmbeddingModel();
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.replaceDocument("doc-1", List.of(chunk("doc-1", "旧内容关于猫。", model)));
        store.replaceDocument("doc-1", List.of(chunk("doc-1", "新内容关于插件注册中心。", model)));

        List<ScoredChunk> hits = store.search(model.embed("插件注册中心"), 3);

        assertEquals(1, hits.size());
        assertTrue(hits.get(0).text().contains("新内容"));
    }

    private static StoredChunk chunk(String docId, String text, HashingEmbeddingModel model) {
        return new StoredChunk(docId, 0, text, model.embed(text));
    }
}
