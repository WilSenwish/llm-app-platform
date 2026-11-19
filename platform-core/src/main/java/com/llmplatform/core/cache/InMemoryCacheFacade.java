package com.llmplatform.core.cache;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 进程内缓存实现。
 * <p>使用 ConcurrentHashMap + 懒过期检查。TTL 到期后在读时自动清理，不单独开定时任务。
 * 单实例开发模式下自动选用，多实例部署应切换 {@link RedisCacheFacade}。
 */
@Slf4j
public class InMemoryCacheFacade implements CacheFacade {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, Object value, long ttlMs) {
        long expireAt = ttlMs > 0 ? System.currentTimeMillis() + ttlMs : -1;
        store.put(key, new Entry(value, expireAt));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expired()) {
            store.remove(key, entry);
            return Optional.empty();
        }
        try {
            return Optional.of((T) entry.value);
        } catch (ClassCastException e) {
            log.warn("[Cache] 类型不匹配 key={} expected={} actual={}",
                    key, type.getSimpleName(), entry.value.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        Iterator<String> it = store.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(prefix)) {
                it.remove();
            }
        }
    }

    private static final class Entry {
        final Object value;
        final long expireAt; // -1 = 不过期

        Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean expired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }
}
