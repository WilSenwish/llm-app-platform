package com.llmplatform.core.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.executor.ExecutionDispatcher;
import com.llmplatform.core.executor.PluginExecutor;
import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.InMemoryPluginRegistry;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformKernelTest {

    @Test
    void handleRoutesRequestThroughThreeLayers() {
        NameCapture capture = new NameCapture();
        ObservationRegistry observations = ObservationRegistry.create();
        observations.observationConfig().observationHandler(capture);

        InMemoryPluginRegistry registry = new InMemoryPluginRegistry();
        registry.register(new RagNamedPlugin());
        PlatformKernel kernel = new PlatformKernel(
                new PreparationOrchestrator(observations, new TaskRouteTable()),
                new ExecutionDispatcher(registry, new PluginExecutor(observations), observations));

        PluginResult<?> result = kernel.handle(
                new PluginRequest("trace-9", "user-9", "rag.query", Map.of("query", "hello")));

        assertTrue(result.success());
        assertEquals("rag-ok", result.data());
        assertTrue(capture.names.contains("orchestration.prepare"));
        assertTrue(capture.names.contains("orchestration.dispatch"));
        assertTrue(capture.names.contains("plugin.execute"));
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

    static class RagNamedPlugin implements AppPlugin {
        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata("plugin-rag", "rag", "1", "rag", PluginType.RAG);
        }

        @Override
        public void init(PluginContext context) {
            // no-op
        }

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            return PluginResult.success("rag-ok");
        }

        @Override
        public void destroy() {
            // no-op
        }
    }
}
