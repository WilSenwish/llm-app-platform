package com.llmplatform.plugin.rag.store;

import com.llmplatform.plugin.rag.embed.HashingEmbeddingModel;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 基于 Redis Hash 的向量存储实现。
 * <p>适合中小规模（<10k chunks）跨实例共享。大规模场景应切换 Milvus。
 * <p>存储结构：
 * <pre>
 * HSET llm.rag.chunk.{docId}.{index} docId {docId} index {index} text {text} vector {base64-floats}
 * </pre>
 */
@Slf4j
public final class RedisSetVectorStore implements VectorStore {

    private static final String CHUNK_KEY_PREFIX = "llm.rag.chunk.";

    private final StringRedisTemplate redis;

    public RedisSetVectorStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void replaceDocument(String docId, List<StoredChunk> documentChunks) {
        // 1. 清理该 docId 的旧块
        String docKeyPattern = CHUNK_KEY_PREFIX + docId + ".*";
        Set<String> oldKeys = redis.keys(docKeyPattern);
        if (oldKeys != null && !oldKeys.isEmpty()) {
            redis.delete(oldKeys);
        }
        // 2. 写入新块
        for (StoredChunk chunk : documentChunks) {
            String chunkKey = CHUNK_KEY_PREFIX + docId + "." + chunk.index();
            String vectorB64 = floatArrayToBase64(chunk.vector());
            redis.opsForHash().putAll(chunkKey, Map.of(
                    "docId", chunk.docId(),
                    "index", String.valueOf(chunk.index()),
                    "text", chunk.text(),
                    "vector", vectorB64));
        }
        log.debug("[RagRedis] replaceDocument docId={} chunks={}", docId, documentChunks.size());
    }

    @Override
    public List<ScoredChunk> search(float[] query, int topK) {
        // 全量扫描（中小规模可接受，大规模换 Milvus）
        Set<String> allKeys = redis.keys(CHUNK_KEY_PREFIX + "*");
        if (allKeys == null || allKeys.isEmpty()) {
            return List.of();
        }

        List<ScoredChunk> scored = new ArrayList<>(allKeys.size());
        for (String key : allKeys) {
            Map<Object, Object> entry = redis.opsForHash().entries(key);
            if (entry.isEmpty()) {
                continue;
            }
            String docId = (String) entry.get("docId");
            String indexStr = (String) entry.get("index");
            String text = (String) entry.get("text");
            String vectorB64 = (String) entry.get("vector");
            if (docId == null || vectorB64 == null) {
                continue;
            }
            float[] vector;
            try {
                vector = base64ToFloatArray(vectorB64);
            } catch (Exception e) {
                log.warn("[RagRedis] vector 解码失败 key={}", key, e);
                continue;
            }
            double score = HashingEmbeddingModel.cosine(query, vector);
            scored.add(new ScoredChunk(docId, Integer.parseInt(indexStr), text, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(Math.max(topK, 0))
                .toList();
    }

    private static String floatArrayToBase64(float[] arr) {
        ByteBuffer buf = ByteBuffer.allocate(arr.length * 4);
        for (float v : arr) {
            buf.putFloat(v);
        }
        return Base64.getEncoder().encodeToString(buf.array());
    }

    private static float[] base64ToFloatArray(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64);
        float[] result = new float[bytes.length / 4];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int i = 0; i < result.length; i++) {
            result[i] = buf.getFloat();
        }
        return result;
    }
}
