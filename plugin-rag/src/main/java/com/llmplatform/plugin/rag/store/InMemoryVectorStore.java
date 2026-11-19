package com.llmplatform.plugin.rag.store;

import com.llmplatform.plugin.rag.embed.HashingEmbeddingModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 进程内向量库。同一 docId 再次入库会替换旧块。
 * <p>数据只活在当前插件实例里，重启即丢失，也不能跨进程共享。第 3 周再换 Milvus。
 */
public final class InMemoryVectorStore implements VectorStore {

    private final List<StoredChunk> chunks = new ArrayList<>();

    /** 用同一文档的新分块替换旧分块。 */
    public synchronized void replaceDocument(String docId, List<StoredChunk> documentChunks) {
        chunks.removeIf(chunk -> chunk.docId().equals(docId));
        chunks.addAll(documentChunks);
    }

    /** 按余弦相似度取前 topK 条，分数从高到低。 */
    public synchronized List<ScoredChunk> search(float[] query, int topK) {
        return chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk.docId(),
                        chunk.index(),
                        chunk.text(),
                        HashingEmbeddingModel.cosine(query, chunk.vector())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(topK, 0))
                .toList();
    }
}
