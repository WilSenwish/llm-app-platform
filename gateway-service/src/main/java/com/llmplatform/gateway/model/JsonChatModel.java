package com.llmplatform.gateway.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.core.model.ModelUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 基于 JDK HttpClient 的 JSON 模型客户端骨架。具体协议由子类填路径和字段。
 */
abstract class JsonChatModel implements ChatModel {

    private final String id;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    JsonChatModel(String id, String baseUrl, Duration timeout, HttpClient httpClient, ObjectMapper objectMapper) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("模型端点 id 不能为空");
        }
        if (baseUrl == null || !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("模型 baseUrl 必须是 http(s) 地址");
        }
        this.id = id;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public final String id() {
        return id;
    }

    protected final ObjectMapper mapper() {
        return objectMapper;
    }

    protected final JsonNode postJson(String path, JsonNode body, Map<String, String> headers) {
        HttpRequest request = request(path, headers)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new ModelUnavailableException(id + " HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (ModelUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelUnavailableException(id + " 调用被中断", ex);
        } catch (IOException ex) {
            throw new ModelUnavailableException(id + " 调用失败", ex);
        }
    }

    protected final void postLines(String path, JsonNode body, Map<String, String> headers, Consumer<String> lineConsumer) {
        HttpRequest request = request(path, headers)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                response.body().close();
                throw new ModelUnavailableException(id + " HTTP " + response.statusCode());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        lineConsumer.accept(line);
                    }
                }
            }
        } catch (ModelUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelUnavailableException(id + " 流式调用被中断", ex);
        } catch (IOException ex) {
            throw new ModelUnavailableException(id + " 流式调用失败", ex);
        }
    }

    protected final void requireGetOk(String path, Map<String, String> headers) {
        HttpRequest request = request(path, headers).GET().build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                throw new ModelUnavailableException(id + " 探测 HTTP " + response.statusCode());
            }
        } catch (ModelUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ModelUnavailableException(id + " 探测被中断", ex);
        } catch (IOException ex) {
            throw new ModelUnavailableException(id + " 探测失败", ex);
        }
    }

    private HttpRequest.Builder request(String path, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        headers.forEach(builder::header);
        return builder;
    }
}
