package com.llmplatform.core.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.orchestration.ExecutionPlan;
import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.InMemoryPluginRegistry;
import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExecutionDispatcherTest {

    @Test
    void returnsNotFoundWhenPluginMissing() {
        ExecutionDispatcher dispatcher = dispatcher(new InMemoryPluginRegistry());
        PluginResult<?> result = dispatcher.dispatch(plan("missing"));

        assertEquals(PlatformErrorCodes.PLUGIN_NOT_FOUND, result.errorCode());
    }

    @Test
    void delegatesToRegisteredPlugin() {
        InMemoryPluginRegistry registry = new InMemoryPluginRegistry();
        registry.register(new EchoPlugin());
        ExecutionDispatcher dispatcher = dispatcher(registry);

        PluginResult<?> result = dispatcher.dispatch(plan("echo"));

        assertTrue(result.success());
        assertEquals("echoed", result.data());
    }

    private static ExecutionDispatcher dispatcher(InMemoryPluginRegistry registry) {
        ObservationRegistry observations = ObservationRegistry.create();
        return new ExecutionDispatcher(registry, new PluginExecutor(observations), observations);
    }

    private static ExecutionPlan plan(String pluginId) {
        return new ExecutionPlan(pluginId, new PluginRequest("trace-1", "user-1", "rag.query", Map.of()), "test");
    }

    static class EchoPlugin implements AppPlugin {
        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata("echo", "echo", "1", "echo", PluginType.CUSTOM);
        }

        @Override
        public void init(PluginContext context) {
            // no-op
        }

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            return PluginResult.success("echoed");
        }

        @Override
        public void destroy() {
            // no-op
        }
    }
}
