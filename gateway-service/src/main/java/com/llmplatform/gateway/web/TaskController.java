package com.llmplatform.gateway.web;

import com.llmplatform.core.orchestration.PlatformKernel;
import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 同步任务入口：网关 → 内核 → 插件。RAG 问答会在插件里再调回模型网关。
 */
@Slf4j
@RestController
public class TaskController {

    private final PlatformKernel kernel;
    private final ObservationRegistry observationRegistry;

    public TaskController(PlatformKernel kernel, ObservationRegistry observationRegistry) {
        this.kernel = kernel;
        this.observationRegistry = observationRegistry;
    }

    @PostMapping("/api/v1/tasks")
    public ResponseEntity<PluginResult<?>> task(@RequestBody(required = false) TaskBody body) {
        if (body == null || isBlank(body.userId()) || isBlank(body.taskType()) || body.payload() == null) {
            return ResponseEntity.badRequest().body(
                    PluginResult.failure(PlatformErrorCodes.INVALID_REQUEST, "需要 userId、taskType 和 payload"));
        }
        String traceId = firstNonBlank(body.traceId(), MDC.get("traceId"), UUID.randomUUID().toString());
        MDC.put("traceId", traceId);
        try {
            PluginResult<?> result = Observation.createNotStarted("gateway.task", observationRegistry)
                    .lowCardinalityKeyValue("task.family", family(body.taskType()))
                    .highCardinalityKeyValue("task.type", body.taskType())
                    .observe(() -> kernel.handle(new PluginRequest(traceId, body.userId(), body.taskType(), body.payload())));
            log.info("[Gateway] task traceId={} userId={} taskType={} success={} errorCode={}",
                    traceId, body.userId(), body.taskType(), result.success(), result.errorCode());
            return ResponseEntity.ok(result);
        } finally {
            MDC.remove("traceId");
        }
    }

    private static String family(String taskType) {
        return taskType.startsWith("rag.") ? "rag" : "other";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    public record TaskBody(String traceId, String userId, String taskType, Map<String, Object> payload) {
    }
}
