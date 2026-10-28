package com.llmplatform.gateway.web;

import com.llmplatform.core.model.ModelRequest;
import com.llmplatform.core.model.ModelStreamListener;
import com.llmplatform.core.model.ModelUnavailableException;
import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.gateway.model.RoutingModelGateway;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 模型健康探测与 SSE 透传。流式接口在首 token 前按端点顺序降级。
 */
@Slf4j
@RestController
public class ModelController {

    static final int MAX_PROMPT_CHARS = 8000;

    private final RoutingModelGateway modelGateway;
    private final ExecutorService streamExecutor;

    public ModelController(
            RoutingModelGateway modelGateway,
            @Qualifier("gatewayStreamExecutor") ExecutorService streamExecutor) {
        this.modelGateway = modelGateway;
        this.streamExecutor = streamExecutor;
    }

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }

    @GetMapping("/api/v1/models/health")
    public List<?> modelHealth() {
        return modelGateway.probe();
    }

    @PostMapping("/api/v1/models/stream")
    public ResponseEntity<?> stream(
            @RequestBody(required = false) StreamBody body,
            @RequestHeader(value = RateLimitFilter.USER_HEADER, required = false) String userId) {
        if (body == null || body.prompt() == null || body.prompt().isBlank()) {
            return ResponseEntity.badRequest().body(
                    PluginResult.failure(PlatformErrorCodes.INVALID_REQUEST, "prompt 不能为空"));
        }
        if (body.prompt().length() > MAX_PROMPT_CHARS) {
            return ResponseEntity.badRequest().body(
                    PluginResult.failure(PlatformErrorCodes.INVALID_REQUEST, "prompt 超过长度限制"));
        }
        String traceId = firstNonBlank(MDC.get("traceId"), UUID.randomUUID().toString());
        SseEmitter emitter = new SseEmitter(60_000L);
        streamExecutor.execute(() -> push(emitter, traceId, userId, body.prompt()));
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(emitter);
    }

    private void push(SseEmitter emitter, String traceId, String userId, String prompt) {
        try {
            modelGateway.stream(new ModelRequest(traceId, userId, prompt), new ModelStreamListener() {
                @Override
                public void onDelta(String modelId, String delta) {
                    send(emitter, "delta", Map.of("modelId", modelId, "text", delta));
                }

                @Override
                public void onComplete(String modelId) {
                    send(emitter, "done", Map.of("modelId", modelId));
                    emitter.complete();
                }
            });
        } catch (ModelUnavailableException ex) {
            log.warn("[Gateway] stream failed traceId={} reason={}", traceId, ex.getMessage());
            emitter.completeWithError(ex);
        } catch (RuntimeException ex) {
            emitter.completeWithError(ex);
        }
    }

    private static void send(SseEmitter emitter, String name, Map<String, String> data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ex) {
            throw new ModelUnavailableException("SSE 写出失败", ex);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    public record StreamBody(String prompt) {
    }
}
