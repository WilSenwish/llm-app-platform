package com.llmplatform.gateway.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.llmplatform.core.model.ModelUnavailableException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ollama {@code /api/chat}。本地和远程 Ollama 只是 baseUrl 不同。
 */
public final class OllamaChatModel extends JsonChatModel {

    private final String model;

    public OllamaChatModel(String id, String baseUrl, String model, Duration timeout) {
        this(id, baseUrl, model, timeout,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                new ObjectMapper());
    }

    OllamaChatModel(String id, String baseUrl, String model, Duration timeout, HttpClient httpClient, ObjectMapper objectMapper) {
        super(id, baseUrl, timeout, httpClient, objectMapper);
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Ollama model 不能为空");
        }
        this.model = model;
    }

    @Override
    public String complete(String prompt) {
        JsonNode root = postJson("/api/chat", body(prompt, false), Map.of());
        String content = root.path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new ModelUnavailableException(id() + " 返回空内容");
        }
        return content;
    }

    @Override
    public void stream(String prompt, Consumer<String> onDelta) {
        postLines("/api/chat", body(prompt, true), Map.of(), line -> emit(line, onDelta));
    }

    @Override
    public void probe() {
        requireGetOk("/api/tags", Map.of());
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
        try {
            String content = mapper().readTree(line).path("message").path("content").asText("");
            if (!content.isEmpty()) {
                onDelta.accept(content);
            }
        } catch (Exception ex) {
            throw new ModelUnavailableException(id() + " 流式内容无法解析", ex);
        }
    }
}
