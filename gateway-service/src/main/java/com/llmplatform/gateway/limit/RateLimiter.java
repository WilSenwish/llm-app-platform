package com.llmplatform.gateway.limit;

/**
 * 网关限流接口。
 * <p>提供全局限流 + 用户限流双重校验。底层实现：进程内（单实例）或 Redis 跨进程（多实例）。
 */
public interface RateLimiter {

    /**
     * 尝试获取配额。
     *
     * @param userId 用户标识（blank/null 按 anonymous 处理）
     * @return {@code true} 放行，{@code false} 拒绝
     */
    boolean tryAcquire(String userId);
}
