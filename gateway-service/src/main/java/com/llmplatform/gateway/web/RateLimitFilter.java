package com.llmplatform.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.gateway.limit.RateLimiter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 对会触发模型或插件执行的 POST 做全局限流和用户限流。配额键是请求头 X-User-Id。
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String USER_HEADER = "X-User-Id";

    private final RateLimiter rateLimiter;
    private final ObservationRegistry observationRegistry;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            ObservationRegistry observationRegistry,
            ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.observationRegistry = observationRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !"/api/v1/tasks".equals(uri) && !"/api/v1/models/stream".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader(USER_HEADER);
        if (rateLimiter.tryAcquire(userId)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("[Gateway] rate limited userId={} uri={}", userId == null ? "anonymous" : userId, request.getRequestURI());
        Observation.createNotStarted("gateway.ratelimit", observationRegistry)
                .lowCardinalityKeyValue("limit.result", "rejected")
                .observe(() -> null);
        JsonResponses.write(response, 429, "RATE_LIMITED", "请求超过配额", objectMapper);
    }
}
