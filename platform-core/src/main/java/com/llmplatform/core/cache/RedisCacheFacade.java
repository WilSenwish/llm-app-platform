package com.llmplatform.core.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 分布式缓存实现。
 * <p>用于多实例部署场景，让 Embedding 缓存、限流状态等跨进程共享。
 * 使用 {@link StringRedisTemplate} + JSON 序列化，避免二进制序列化兼容性问题。
 */
@Slf4j
public class RedisCacheFacade implements CacheFacade {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final String keyPrefix; // 统一前缀，避免与其他业务冲突

    public RedisCacheFacade(StringRedisTemplate redis, ObjectMapper mapper, String keyPrefix) {
        this.redis = redis;
        this.mapper = mapper;
        this.keyPrefix = keyPrefix == null ? "llm.cache." : keyPrefix;
    }

    @Override
    public void put(String key, Object value, long ttlMs) {
        try {
            String json = value instanceof String ? (String) value : mapper.writeValueAsString(value);
            String fullKey = keyPrefix + key;
            if (ttlMs > 0) {
                redis.opsForValue().set(fullKey, json, ttlMs, TimeUnit.MILLISECONDS);
            } else {
                redis.opsForValue().set(fullKey, json);
            }
        } catch (JsonProcessingException e) {
            log.warn("[Cache] JSON 序列化失败 key={}", key, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        String fullKey = keyPrefix + key;
        String json;
        try {
            json = redis.opsForValue().get(fullKey);
        } catch (Exception e) {
            log.warn("[Cache] Redis GET 失败 key={}", key, e);
            return Optional.empty();
        }
        if (json == null) {
            return Optional.empty();
        }
        if (type == String.class) {
            return Optional.of((T) json);
        }
        try {
            return Optional.of(mapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("[Cache] JSON 反序列化失败 key={} type={}", key, type.getSimpleName(), e);
            return Optional.empty();
        }
    }

    @Override
    public void evict(String key) {
        redis.delete(keyPrefix + key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        String pattern = keyPrefix + prefix + "*";
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            log.warn("[Cache] KEYS 扫描失败 pattern={}", pattern, e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            redis.hasKey(keyPrefix + "healthcheck");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
