package com.llmplatform.gateway.limit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * 全局桶 + 用户桶。两边都有余量才放行。桶不跨进程，多实例各自计数。
 * <p>单实例默认实现。多实例部署应切换 {@link RedisTokenBucketRateLimiter}。
 */
public final class RequestRateLimiter implements RateLimiter {

    static final int MAX_USERS = 4096;

    private final TokenBucket global;
    private final double userRate;
    private final LongSupplier clock;
    private final Map<String, TokenBucket> users = new HashMap<>();

    public RequestRateLimiter(int globalPermitsPerSecond, int userPermitsPerSecond) {
        this(globalPermitsPerSecond, userPermitsPerSecond, System::nanoTime);
    }

    public RequestRateLimiter(int globalPermitsPerSecond, int userPermitsPerSecond, LongSupplier clock) {
        double globalRate = atLeastOne(globalPermitsPerSecond);
        this.userRate = atLeastOne(userPermitsPerSecond);
        this.clock = clock;
        this.global = new TokenBucket(globalRate, clock.getAsLong());
    }

    /** 返回 false 表示应拒绝。空白用户记到 anonymous，共用一个桶。 */
    public synchronized boolean tryAcquire(String userId) {
        String key = userId == null || userId.isBlank() ? "anonymous" : userId;
        if (users.size() >= MAX_USERS && !users.containsKey(key)) {
            return false;
        }
        long now = clock.getAsLong();
        TokenBucket user = users.computeIfAbsent(key, id -> new TokenBucket(userRate, now));
        global.refill(now);
        user.refill(now);
        if (global.available() < 1 || user.available() < 1) {
            return false;
        }
        global.consume();
        user.consume();
        return true;
    }

    private static double atLeastOne(int permits) {
        return Math.max(1, permits);
    }

    private static final class TokenBucket {

        private final double ratePerSecond;
        private final double capacity;
        private double tokens;
        private long lastNanos;

        private TokenBucket(double ratePerSecond, long nowNanos) {
            this.ratePerSecond = ratePerSecond;
            this.capacity = ratePerSecond;
            this.tokens = ratePerSecond;
            this.lastNanos = nowNanos;
        }

        private void refill(long nowNanos) {
            double elapsedSeconds = (nowNanos - lastNanos) / 1_000_000_000d;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * ratePerSecond);
                lastNanos = nowNanos;
            }
        }

        private double available() {
            return tokens;
        }

        private void consume() {
            tokens -= 1;
        }
    }
}
