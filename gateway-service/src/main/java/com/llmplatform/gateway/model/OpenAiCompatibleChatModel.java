package com.llmplatform.gateway.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.llmplatform.core.model.ModelUnavailableException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容的 {@code /v1/chat/completions}。远端端点走这条协议。
 */
public final class OpenAiCompatibleChatModel extends JsonChatModel {

    private final String model;
    private final String apiKey;

    public OpenAiCompatibleChatModel(String id, String baseUrl, String model, String apiKey, Duration timeout) {
        this(id, baseUrl, model, apiKey, timeout,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                new ObjectMapper());
    }

    OpenAiCompatibleChatModel(
            String id,
            String baseUrl,
            String model,
            String apiKey,
            Duration timeout,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        super(id, baseUrl, timeout, httpClient, objectMapper);
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("OpenAI model 不能为空");
        }
        this.model = model;
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    public String complete(String prompt) {
        JsonNode root = postJson("/v1/chat/completions", body(prompt, false), headers());
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new ModelUnavailableException(id() + " 返回空内容");
        }
        return content;
    }

    @Override
    public void stream(String prompt, Consumer<String> onDelta) {
        postLines("/v1/chat/completions", body(prompt, true), headers(), line -> emit(line, onDelta));
    }

    @Override
    public void probe() {
        requireGetOk("/v1/models", headers());
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (!apiKey.isBlank()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    private ObjectNode body(String prompt, boolean stream) {
        ObjectNode body = mapper().createObjectNode();
        body.put("model", model);
        body.put("stream", stream);
        ObjectNode message = body.putArray("messages").addObject();
        message.put("role", "user");
        message.put("content", prompt);
        return body;
    }

    private void emit(String line, Consumer<String> onDelta) {
        if ("data: [DONE]".equals(line.trim())) {
            return;
        }
        String json = line.startsWith("data:") ? line.substring("data:".length()).trim() : line.trim();
        if (json.isEmpty() || "[DONE]".equals(json)) {
            return;
        }
        try {
            String content = mapper().readTree(json).path("choices").path(0).path("delta").path("content").asText("");
            if (!content.isEmpty()) {
                onDelta.accept(content);
            }
        } catch (Exception ex) {
            throw new ModelUnavailableException(id() + " 流式内容无法解析", ex);
        }
    }
}
