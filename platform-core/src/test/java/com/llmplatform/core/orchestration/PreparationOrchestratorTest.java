package com.llmplatform.core.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginRequest;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreparationOrchestratorTest {

    private PreparationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PreparationOrchestrator(ObservationRegistry.create(), new TaskRouteTable());
    }

    @Test
    void routesRagTaskToRagPlugin() {
        PreparationOutcome outcome = orchestrator.prepare(request("rag.query", Map.of("query", "hello")));

        assertTrue(outcome.accepted());
        assertEquals("plugin-rag", outcome.plan().pluginId());
        assertEquals("task-prefix", outcome.plan().routeReason());
    }

    @Test
    void explicitPluginIdOverridesTaskPrefix() {
        PreparationOutcome outcome = orchestrator.prepare(
                request("rag.query", Map.of("pluginId", "plugin-custom", "query", "hello")));

        assertTrue(outcome.accepted());
        assertEquals("plugin-custom", outcome.plan().pluginId());
        assertEquals("explicit-plugin-id", outcome.plan().routeReason());
    }

    @Test
    void rejectsBlankTaskType() {
        PreparationOutcome outcome = orchestrator.prepare(request("  ", Map.of()));

        assertFalse(outcome.accepted());
        assertEquals(PlatformErrorCodes.INVALID_REQUEST, outcome.error().errorCode());
    }

    @Test
    void rejectsUnknownTask() {
        PreparationOutcome outcome = orchestrator.prepare(request("chat.complete", Map.of()));

        assertFalse(outcome.accepted());
        assertEquals(PlatformErrorCodes.ROUTE_NOT_FOUND, outcome.error().errorCode());
    }

    @Test
    void rejectsNullRequest() {
        PreparationOutcome outcome = orchestrator.prepare(null);

        assertFalse(outcome.accepted());
        assertEquals(PlatformErrorCodes.INVALID_REQUEST, outcome.error().errorCode());
    }

    private static PluginRequest request(String taskType, Map<String, Object> payload) {
        return new PluginRequest("trace-1", "user-1", taskType, payload);
    }
}
