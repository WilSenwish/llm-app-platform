package com.llmplatform.core.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存门面自动配置。
 * <p>Redis 可用时自动选用 {@link RedisCacheFacade}，否则 fallback 到 {@link InMemoryCacheFacade}。
 * 这样本地开发无需 Redis 也能跑，生产加 Redis 即自动切换。
 */
@Configuration
public class CacheAutoConfiguration {

    /** 无 Redis → 进程内缓存（开发默认）。 */
    @Bean
    @ConditionalOnMissingBean({CacheFacade.class, StringRedisTemplate.class})
    public CacheFacade inMemoryCacheFacade() {
        return new InMemoryCacheFacade();
    }

    /** 有 Redis → 分布式缓存（生产）。 */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(CacheFacade.class)
    public CacheFacade redisCacheFacade(StringRedisTemplate redis, ObjectMapper mapper) {
        return new RedisCacheFacade(redis, mapper, "llm.cache.");
    }
}
