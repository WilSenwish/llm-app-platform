package com.llmplatform.core.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 平台级缓存门面。
 * <p>为 Embedding 缓存、RAG 召回结果缓存等场景提供统一抽象，底层可切换进程内缓存或 Redis。
 * <p>生命周期：
 * <ul>
 *   <li>单机开发 / 无 Redis → 默认 {@link InMemoryCacheFacade}（进程内 ConcurrentHashMap）</li>
 *   <li>生产多实例 / 有 Redis → {@link RedisCacheFacade}</li>
 * </ul>
 */
public interface CacheFacade {

    /**
     * 写入缓存。
     *
     * @param key    缓存键（建议统一前缀如 "llm.embedding."）
     * @param value  缓存值
     * @param ttlMs  生存时间，毫秒。{@code <= 0} 表示不过期
     */
    void put(String key, Object value, long ttlMs);

    /**
     * 快捷写入（默认不过期）。
     */
    default void put(String key, Object value) {
        put(key, value, -1);
    }

    /** 读取缓存，类型不匹配或不存在返回 {@link Optional#empty()}。 */
    <T> Optional<T> get(String key, Class<T> type);

    /** 删除指定 key。 */
    void evict(String key);

    /** 清空指定前缀下所有缓存（支持通配符 "llm.embedding.*"）。 */
    void evictByPrefix(String prefix);

    /** 判断是否可用（Redis 连通性检查）。 */
    default boolean isAvailable() {
        return true;
    }
}
