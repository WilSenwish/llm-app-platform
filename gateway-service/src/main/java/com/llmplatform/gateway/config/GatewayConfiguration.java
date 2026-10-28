package com.llmplatform.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.gateway.limit.RequestRateLimiter;
import com.llmplatform.gateway.model.ChatModel;
import com.llmplatform.gateway.model.ModelCatalog;
import com.llmplatform.gateway.model.OllamaChatModel;
import com.llmplatform.gateway.model.OpenAiCompatibleChatModel;
import com.llmplatform.gateway.model.RoutingModelGateway;
import com.llmplatform.gateway.web.ApiKeyFilter;
import com.llmplatform.gateway.web.RateLimitFilter;
import com.llmplatform.gateway.web.TraceMdcFilter;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {

    @Bean
    @ConditionalOnMissingBean(ModelCatalog.class)
    public ModelCatalog modelCatalog(GatewayProperties properties) {
        return () -> createModels(properties);
    }

    @Bean(destroyMethod = "close")
    public RoutingModelGateway routingModelGateway(
            ModelCatalog modelCatalog,
            ObservationRegistry observationRegistry,
            GatewayProperties properties) {
        Duration timeout = properties.getModelTimeout() == null ? Duration.ofSeconds(3) : properties.getModelTimeout();
        return new RoutingModelGateway(
                modelCatalog.models(),
                observationRegistry,
                properties.getMaxAttempts(),
                timeout);
    }

    @Bean
    public RequestRateLimiter requestRateLimiter(GatewayProperties properties) {
        return new RequestRateLimiter(properties.getGlobalPermitsPerSecond(), properties.getUserPermitsPerSecond());
    }

    @Bean(name = "gatewayStreamExecutor", destroyMethod = "shutdown")
    public ExecutorService gatewayStreamExecutor() {
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "gateway-stream");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    public FilterRegistrationBean<TraceMdcFilter> traceMdcFilter() {
        FilterRegistrationBean<TraceMdcFilter> registration = new FilterRegistrationBean<>(new TraceMdcFilter());
        registration.setOrder(0);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(GatewayProperties properties, ObjectMapper objectMapper) {
        FilterRegistrationBean<ApiKeyFilter> registration =
                new FilterRegistrationBean<>(new ApiKeyFilter(properties.getApiKey(), objectMapper));
        registration.setOrder(1);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RequestRateLimiter rateLimiter,
            ObservationRegistry observationRegistry,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(rateLimiter, observationRegistry, objectMapper));
        registration.setOrder(2);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    private static List<ChatModel> createModels(GatewayProperties properties) {
        if (properties.getEndpoints() == null || properties.getEndpoints().isEmpty()) {
            return List.of();
        }
        Duration timeout = properties.getModelTimeout() == null ? Duration.ofSeconds(3) : properties.getModelTimeout();
        List<ChatModel> models = new ArrayList<>();
        for (GatewayProperties.Endpoint endpoint : properties.getEndpoints()) {
            String provider = endpoint.getProvider() == null ? "" : endpoint.getProvider().trim().toLowerCase();
            models.add(switch (provider) {
                case "ollama" -> new OllamaChatModel(endpoint.getId(), endpoint.getBaseUrl(), endpoint.getModel(), timeout);
                case "openai" -> new OpenAiCompatibleChatModel(
                        endpoint.getId(), endpoint.getBaseUrl(), endpoint.getModel(), endpoint.getApiKey(), timeout);
                default -> throw new IllegalArgumentException("不支持的模型 provider: " + endpoint.getProvider());
            });
        }
        return List.copyOf(models);
    }
}
