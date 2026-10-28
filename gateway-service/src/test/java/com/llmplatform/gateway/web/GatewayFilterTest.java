package com.llmplatform.gateway.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.gateway.limit.RequestRateLimiter;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GatewayFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsWrongApiKeyAndSkipsHealth() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret", objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tasks");
        request.addHeader(ApiKeyFilter.HEADER, "nope");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean passed = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> passed.set(true));

        assertEquals(401, response.getStatus());
        assertFalse(passed.get());
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));

        MockHttpServletRequest health = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse healthResponse = new MockHttpServletResponse();
        AtomicBoolean healthPassed = new AtomicBoolean();
        filter.doFilter(health, healthResponse, (req, res) -> healthPassed.set(true));
        assertTrue(healthPassed.get());
    }

    @Test
    void blankApiKeyDoesNotBlock() throws ServletException, IOException {
        ApiKeyFilter filter = new ApiKeyFilter("", objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tasks");
        AtomicBoolean passed = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passed.set(true));

        assertTrue(passed.get());
    }

    @Test
    void secondPostIsRateLimited() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(
                new RequestRateLimiter(10, 1), ObservationRegistry.create(), objectMapper);
        assertEquals(200, post(filter).getStatus());
        MockHttpServletResponse second = post(filter);

        assertEquals(429, second.getStatus());
        assertTrue(second.getContentAsString().contains("RATE_LIMITED"));
    }

    private static MockHttpServletResponse post(RateLimitFilter filter) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tasks");
        request.addHeader(RateLimitFilter.USER_HEADER, "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(200));
        return response;
    }
}
