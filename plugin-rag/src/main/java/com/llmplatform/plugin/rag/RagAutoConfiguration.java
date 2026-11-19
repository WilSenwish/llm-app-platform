package com.llmplatform.plugin.rag;

import com.llmplatform.plugin.rag.store.RedisSetVectorStore;
import com.llmplatform.plugin.rag.store.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RAG 插件存储自动配置。
 * <p>有 Redis 时注册 {@link RedisSetVectorStore}，否则由 {@code RagPlugin} 内部 fallback 到内存实现。
 */
@Configuration
public class RagAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore redisVectorStore(StringRedisTemplate redis) {
        return new RedisSetVectorStore(redis);
    }
}
