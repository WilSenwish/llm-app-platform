package com.llmplatform.core.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.DefaultPluginContext;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PluginExecutorTest {

    @Test
    void executeReturnsPluginResultAndWrapsObservation() {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        PluginExecutor executor = new PluginExecutor(observationRegistry);
        PluginContextStub context = new PluginContextStub(observationRegistry);

        PluginResult<?> result = executor.execute(
                new RagLikePlugin(),
                new PluginRequest("trace-1", "user-1", "rag", Map.of()),
                context);

        assertTrue(result.success());
        assertEquals("done", result.data());
    }

    /** 测试用上下文：复用默认实现。 */
    static class PluginContextStub extends DefaultPluginContext {
        PluginContextStub(ObservationRegistry registry) {
            super("trace-1", "user-1", registry);
        }
    }

    static class RagLikePlugin implements AppPlugin {
        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata("p", "p", "1", "p", PluginType.RAG);
        }

        @Override
        public void init(PluginContext context) {
            // no-op
        }

        @Override
        public PluginResult<?> execute(PluginRequest r) {
            return PluginResult.success("done");
        }

        @Override
        public void destroy() {
            // no-op
        }
    }
}
