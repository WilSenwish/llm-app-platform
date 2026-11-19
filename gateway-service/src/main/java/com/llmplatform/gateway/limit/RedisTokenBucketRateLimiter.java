package com.llmplatform.gateway.limit;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 基于 Redis 的跨进程令牌桶限流实现。
 * <p>用 Lua 脚本保证「计算 refill → 判断 → 扣减 → 写回」原子执行。
 * 每个桶一个 key，TTL = 桶容量秒数 × 3（自动清理不活跃用户）。
 */
@Slf4j
public final class RedisTokenBucketRateLimiter implements RateLimiter {

    private static final String GLOBAL_KEY = "llm.gateway.rate.global";
    private static final String USER_KEY_PREFIX = "llm.gateway.rate.user.";

    /** Lua 脚本：原子性 refill + tryAcquire。返回 1=放行, 0=拒绝。 */
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local ratePerMs = tonumber(ARGV[2])
            local nowMs = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])
            local ttlMs = tonumber(ARGV[5])
            
            local data = redis.call('HMGET', key, 'tokens', 'lastRefillMs')
            local tokens = tonumber(data[1])
            local lastRefillMs = tonumber(data[2])
            
            if tokens == nil then
                -- 首次初始化，桶满
                tokens = capacity
                lastRefillMs = nowMs
            else
                -- refill
                local elapsedMs = nowMs - lastRefillMs
                if elapsedMs > 0 then
                    tokens = math.min(capacity, tokens + elapsedMs * ratePerMs)
                    lastRefillMs = nowMs
                end
            end
            
            if tokens >= requested then
                tokens = tokens - requested
                redis.call('HMSET', key, 'tokens', tokens, 'lastRefillMs', lastRefillMs)
                redis.call('EXPIRE', key, math.ceil(ttlMs / 1000))
                return 1
            end
            
            -- 不够，只更新 lastRefillMs 避免过期
            redis.call('HMSET', key, 'tokens', tokens, 'lastRefillMs', lastRefillMs)
            redis.call('EXPIRE', key, math.ceil(ttlMs / 1000))
            return 0
            """,
            Long.class);

    private final StringRedisTemplate redis;
    private final double globalRatePerSec;
    private final double userRatePerSec;
    private final long capacitySec; // 桶容量（秒），同时作为 TTL 基数
    private final LongSupplier clockMs;

    public RedisTokenBucketRateLimiter(
            StringRedisTemplate redis,
            int globalPermitsPerSecond,
            int userPermitsPerSecond) {
        this(redis, globalPermitsPerSecond, userPermitsPerSecond, Duration.ofSeconds(60));
    }

    public RedisTokenBucketRateLimiter(
            StringRedisTemplate redis,
            int globalPermitsPerSecond,
            int userPermitsPerSecond,
            Duration bucketTtl) {
        this.redis = redis;
        this.globalRatePerSec = atLeastOne(globalPermitsPerSecond);
        this.userRatePerSec = atLeastOne(userPermitsPerSecond);
        this.capacitySec = bucketTtl.getSeconds();
        this.clockMs = System::currentTimeMillis;
    }

    @Override
    public boolean tryAcquire(String userId) {
        String key = (userId == null || userId.isBlank() ? "anonymous" : userId);
        String userKey = USER_KEY_PREFIX + key;

        long nowMs = clockMs.getAsLong();
        double globalRatePerMs = globalRatePerSec / 1000.0;
        double userRatePerMs = userRatePerSec / 1000.0;
        long bucketTtlMs = (long) capacitySec * 1000;

        try {
            // 全局桶
            Long globalOk = redis.execute(ACQUIRE_SCRIPT,
                    List.of(GLOBAL_KEY),
                    String.valueOf(globalRatePerSec),   // capacity（允许满桶 N 秒 × 每秒补的数）
                    String.valueOf(globalRatePerMs),    // rate per ms
                    String.valueOf(nowMs),
                    "1",                                  // requested
                    String.valueOf(bucketTtlMs));

            if (globalOk == null || globalOk != 1L) {
                return false;
            }

            // 用户桶（Redis 异常时降级放行——避免限流组件本身成故障点）
            Long userOk = redis.execute(ACQUIRE_SCRIPT,
                    List.of(userKey),
                    String.valueOf(userRatePerSec),
                    String.valueOf(userRatePerMs),
                    String.valueOf(nowMs),
                    "1",
                    String.valueOf(bucketTtlMs));

            return userOk != null && userOk == 1L;
        } catch (Exception e) {
            log.warn("[RateLimit] Redis 异常降级放行 key={} err={}", key, e.getMessage());
            return true; // Redis 不可用时放行，让网关降级而非阻断
        }
    }

    private static double atLeastOne(int permits) {
        return Math.max(1, permits);
    }
}
