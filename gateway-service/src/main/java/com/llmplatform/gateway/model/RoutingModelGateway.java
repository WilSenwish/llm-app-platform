package com.llmplatform.gateway.model;

import com.llmplatform.core.model.ModelCompletion;
import com.llmplatform.core.model.ModelGateway;
import com.llmplatform.core.model.ModelRequest;
import com.llmplatform.core.model.ModelStreamListener;
import com.llmplatform.core.model.ModelUnavailableException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * 按端点顺序调用。同一端点先重试，仍失败就标停并换下一个。首 token 发出后不再换端点。
 */
@Slf4j
public final class RoutingModelGateway implements ModelGateway, AutoCloseable {

    private final List<ChatModel> models;
    private final ObservationRegistry observationRegistry;
    private final ModelHealth health = new ModelHealth();
    private final int maxAttempts;
    private final Duration timeout;
    private final ExecutorService executor;

    public RoutingModelGateway(
            List<ChatModel> models,
            ObservationRegistry observationRegistry,
            int maxAttempts,
            Duration timeout) {
        this.models = List.copyOf(models);
        this.observationRegistry = observationRegistry;
        this.maxAttempts = Math.min(3, Math.max(1, maxAttempts));
        this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        this.executor = Executors.newFixedThreadPool(Math.max(2, Math.min(8, Math.max(this.models.size(), 1))), runnable -> {
            Thread thread = new Thread(runnable, "gateway-model");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public ModelCompletion complete(ModelRequest request) {
        requirePrompt(request);
        Observation route = Observation.createNotStarted("gateway.model.route", observationRegistry)
                .highCardinalityKeyValue("trace.id", emptyIfNull(request.traceId()));
        return route.observe(() -> doComplete(request));
    }

    @Override
    public void stream(ModelRequest request, ModelStreamListener listener) {
        requirePrompt(request);
        if (listener == null) {
            throw new ModelUnavailableException("流式监听器不能为空");
        }
        Observation route = Observation.createNotStarted("gateway.model.route", observationRegistry)
                .highCardinalityKeyValue("trace.id", emptyIfNull(request.traceId()));
        route.observe(() -> {
            doStream(request, listener);
            return null;
        });
    }

    /** 探测全部端点并刷新健康标记。 */
    public List<ModelHealthView> probe() {
        List<ModelHealthView> views = new ArrayList<>();
        for (ChatModel model : models) {
            try {
                model.probe();
                health.markUp(model.id());
            } catch (RuntimeException ex) {
                health.markDown(model.id());
                log.warn("[ModelGateway] probe down modelId={} reason={}", model.id(), ex.getMessage());
            }
            views.add(new ModelHealthView(model.id(), health.allow(model.id())));
        }
        return List.copyOf(views);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private ModelCompletion doComplete(ModelRequest request) {
        ModelUnavailableException last = null;
        boolean failedOver = false;
        for (ChatModel model : models) {
            if (!health.allow(model.id())) {
                failedOver = true;
                continue;
            }
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                Observation call = Observation.createNotStarted("gateway.model.call", observationRegistry)
                        .lowCardinalityKeyValue("model.id", model.id())
                        .lowCardinalityKeyValue("model.attempt", Integer.toString(attempt));
                try {
                    int currentAttempt = attempt;
                    String text = call.observe(() -> invoke(() -> model.complete(request.prompt()), model.id(), currentAttempt));
                    if (failedOver) {
                        log.warn("[ModelGateway] failover modelId={} traceId={}", model.id(), request.traceId());
                    } else {
                        log.info("[ModelGateway] complete modelId={} traceId={}", model.id(), request.traceId());
                    }
                    return new ModelCompletion(model.id(), text);
                } catch (ModelUnavailableException ex) {
                    last = ex;
                    log.warn("[ModelGateway] call failed modelId={} attempt={} traceId={} reason={}",
                            model.id(), attempt, request.traceId(), ex.getMessage());
                }
            }
            health.markDown(model.id());
            failedOver = true;
        }
        throw last != null ? last : new ModelUnavailableException("没有可用模型");
    }

    private void doStream(ModelRequest request, ModelStreamListener listener) {
        ModelUnavailableException last = null;
        for (ChatModel model : models) {
            if (!health.allow(model.id())) {
                continue;
            }
            AtomicBoolean started = new AtomicBoolean(false);
            Observation call = Observation.createNotStarted("gateway.model.call", observationRegistry)
                    .lowCardinalityKeyValue("model.id", model.id())
                    .lowCardinalityKeyValue("model.attempt", "1");
            try {
                call.observe(() -> {
                    invoke(() -> {
                        model.stream(request.prompt(), delta -> {
                            started.set(true);
                            listener.onDelta(model.id(), delta);
                        });
                        return "ok";
                    }, model.id(), 1);
                    return null;
                });
                listener.onComplete(model.id());
                log.info("[ModelGateway] stream done modelId={} traceId={}", model.id(), request.traceId());
                return;
            } catch (ModelUnavailableException ex) {
                if (started.get()) {
                    throw ex;
                }
                health.markDown(model.id());
                last = ex;
                log.warn("[ModelGateway] stream failover modelId={} traceId={} reason={}",
                        model.id(), request.traceId(), ex.getMessage());
            }
        }
        throw last != null ? last : new ModelUnavailableException("没有可用模型");
    }

    private String invoke(Callable<String> callable, String modelId, int attempt) {
        Future<String> future = executor.submit(callable);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new ModelUnavailableException(modelId + " 第 " + attempt + " 次调用超时", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof ModelUnavailableException unavailable) {
                throw unavailable;
            }
            throw new ModelUnavailableException(modelId + " 调用失败", cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new ModelUnavailableException(modelId + " 调用被中断", ex);
        }
    }

    private static void requirePrompt(ModelRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new ModelUnavailableException("prompt 不能为空");
        }
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
