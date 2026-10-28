package com.llmplatform.gateway.limit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    @Test
    void globalBucketBlocksASecondUserUntilRefill() {
        AtomicLong now = new AtomicLong(1_000);
        RequestRateLimiter limiter = new RequestRateLimiter(1, 10, now::get);

        assertTrue(limiter.tryAcquire("alice"));
        assertFalse(limiter.tryAcquire("bob"));

        now.addAndGet(1_000_000_000L);
        assertTrue(limiter.tryAcquire("bob"));
    }

    @Test
    void userBucketIsIndependent() {
        AtomicLong now = new AtomicLong(1_000);
        RequestRateLimiter limiter = new RequestRateLimiter(10, 1, now::get);

        assertTrue(limiter.tryAcquire("alice"));
        assertFalse(limiter.tryAcquire("alice"));
        assertTrue(limiter.tryAcquire("bob"));
    }
}
