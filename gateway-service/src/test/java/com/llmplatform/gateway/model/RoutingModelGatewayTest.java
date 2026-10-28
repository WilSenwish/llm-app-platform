package com.llmplatform.gateway.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.model.ModelCompletion;
import com.llmplatform.core.model.ModelRequest;
import com.llmplatform.core.model.ModelStreamListener;
import com.llmplatform.core.model.ModelUnavailableException;
import com.llmplatform.gateway.support.ScriptedChatModel;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoutingModelGatewayTest {

    @Test
    void fallsBackWhenPrimaryFailsAndThenSkipsIt() {
        ScriptedChatModel primary = new ScriptedChatModel("ollama-local", () -> {
            throw new ModelUnavailableException("down");
        }, List.of());
        ScriptedChatModel fallback = new ScriptedChatModel("remote-openai", () -> "from-fallback", List.of("a"));
        try (RoutingModelGateway gateway = gateway(List.of(primary, fallback), 1, Duration.ofSeconds(1))) {
            ModelCompletion first = gateway.complete(new ModelRequest("t1", "u", "hello"));
            ModelCompletion second = gateway.complete(new ModelRequest("t2", "u", "hello"));

            assertEquals("remote-openai", first.modelId());
            assertEquals("from-fallback", first.text());
            assertEquals("remote-openai", second.modelId());
            assertEquals(1, primary.completeCalls());
            assertEquals(2, fallback.completeCalls());
        }
    }

    @Test
    void retriesSameModelBeforeFailover() {
        AtomicInteger remaining = new AtomicInteger(1);
        ScriptedChatModel primary = new ScriptedChatModel("ollama-local", () -> {
            if (remaining.getAndDecrement() > 0) {
                throw new ModelUnavailableException("once");
            }
            return "recovered";
        }, List.of());
        try (RoutingModelGateway gateway = gateway(List.of(primary), 2, Duration.ofSeconds(1))) {
            ModelCompletion completion = gateway.complete(new ModelRequest("t", "u", "hello"));

            assertEquals("ollama-local", completion.modelId());
            assertEquals("recovered", completion.text());
            assertEquals(2, primary.completeCalls());
        }
    }

    @Test
    void timeoutFallsOverToNextModel() {
        ScriptedChatModel slow = new ScriptedChatModel("ollama-local", () -> "too-late", List.of()).sleepMs(5_000);
        ScriptedChatModel fallback = new ScriptedChatModel("remote-openai", () -> "fast", List.of());
        try (RoutingModelGateway gateway = gateway(List.of(slow, fallback), 1, Duration.ofMillis(100))) {
            ModelCompletion completion = assertTimeoutPreemptively(Duration.ofSeconds(2),
                    () -> gateway.complete(new ModelRequest("t", "u", "hello")));

            assertEquals("remote-openai", completion.modelId());
            assertEquals("fast", completion.text());
        }
    }

    @Test
    void streamFallsBackBeforeFirstToken() {
        ScriptedChatModel primary = new ScriptedChatModel("ollama-local", () -> {
            throw new ModelUnavailableException("down");
        }, List.of());
        ScriptedChatModel fallback = new ScriptedChatModel("remote-openai", () -> "ok", List.of("你", "好"));
        List<String> deltas = new ArrayList<>();
        List<String> completed = new ArrayList<>();
        try (RoutingModelGateway gateway = gateway(List.of(primary, fallback), 1, Duration.ofSeconds(1))) {
            gateway.stream(new ModelRequest("t", "u", "hi"), new ModelStreamListener() {
                @Override
                public void onDelta(String modelId, String delta) {
                    deltas.add(modelId + ":" + delta);
                }

                @Override
                public void onComplete(String modelId) {
                    completed.add(modelId);
                }
            });
        }

        assertEquals(List.of("remote-openai:你", "remote-openai:好"), deltas);
        assertEquals(List.of("remote-openai"), completed);
    }

    @Test
    void probeRestoresADownModel() {
        ScriptedChatModel primary = new ScriptedChatModel("ollama-local", () -> {
            throw new ModelUnavailableException("down");
        }, List.of()).probeFails(true);
        try (RoutingModelGateway gateway = gateway(List.of(primary), 1, Duration.ofSeconds(1))) {
            assertThrows(ModelUnavailableException.class, () -> gateway.complete(new ModelRequest("t", "u", "hi")));
            assertTrue(gateway.probe().stream().noneMatch(ModelHealthView::up));

            primary.probeFails(false);
            assertTrue(gateway.probe().get(0).up());
        }
    }

    @Test
    void recordsRouteAndCallSpans() {
        NameCapture capture = new NameCapture();
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(capture);
        ScriptedChatModel model = new ScriptedChatModel("ollama-local", () -> "ok", List.of());
        try (RoutingModelGateway gateway = new RoutingModelGateway(List.of(model), registry, 1, Duration.ofSeconds(1))) {
            gateway.complete(new ModelRequest("t", "u", "hi"));
        }

        assertTrue(capture.names.contains("gateway.model.route"));
        assertTrue(capture.names.contains("gateway.model.call"));
    }

    private static RoutingModelGateway gateway(List<ChatModel> models, int attempts, Duration timeout) {
        return new RoutingModelGateway(models, ObservationRegistry.create(), attempts, timeout);
    }

    static final class NameCapture implements ObservationHandler<Observation.Context> {
        private final List<String> names = new ArrayList<>();

        @Override
        public void onStart(Observation.Context context) {
            names.add(context.getName());
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }
}
