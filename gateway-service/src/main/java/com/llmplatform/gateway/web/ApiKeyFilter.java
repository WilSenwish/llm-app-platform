package com.llmplatform.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 简单 API Key。配置为空时不拦截，方便本地。比较时不把密钥写进日志。
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/api/v1/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String presented = request.getHeader(HEADER);
        if (!matches(apiKey, presented)) {
            JsonResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "缺少或错误的 X-Api-Key", objectMapper);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean matches(String expected, String actual) {
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual == null ? new byte[0] : actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }
}
