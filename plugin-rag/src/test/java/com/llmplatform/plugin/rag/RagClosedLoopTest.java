package com.llmplatform.plugin.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.executor.ExecutionDispatcher;
import com.llmplatform.core.executor.PluginExecutor;
import com.llmplatform.core.orchestration.PlatformKernel;
import com.llmplatform.core.orchestration.PreparationOrchestrator;
import com.llmplatform.core.orchestration.TaskRouteTable;
import com.llmplatform.core.plugin.DefaultPluginContext;
import com.llmplatform.core.plugin.InMemoryPluginRegistry;
import com.llmplatform.core.plugin.PluginLifecycle;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.SpiPluginLoader;
import com.llmplatform.plugin.rag.answer.QueryResult;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RagClosedLoopTest {

    @Test
    void ingestThenQueryReturnsEvidenceFromThatDocument() {
        RagPlugin plugin = new RagPlugin();
        plugin.init(new DefaultPluginContext("trace-1", "user-1", ObservationRegistry.create()));

        PluginResult<?> ingested = plugin.execute(new PluginRequest(
                "trace-1",
                "user-1",
                "rag.ingest",
                Map.of("docId", "doc-1", "text", "平台内核通过插件注册中心加载 RAG 插件。")));
        PluginResult<?> queried = plugin.execute(new PluginRequest(
                "trace-1",
                "user-1",
                "rag.query",
                Map.of("query", "RAG 插件如何被加载")));

        assertTrue(ingested.success());
        assertTrue(queried.success());
        QueryResult result = (QueryResult) queried.data();
        assertTrue(result.answer().contains("插件注册中心"));
        assertEquals("doc-1", result.hits().get(0).docId());
    }

    @Test
    void unrelatedQueryReturnsNoEvidence() {
        RagPlugin plugin = new RagPlugin();
        plugin.init(new DefaultPluginContext("trace-1", "user-1", ObservationRegistry.create()));
        plugin.execute(new PluginRequest(
                "trace-1",
                "user-1",
                "rag.ingest",
                Map.of("docId", "doc-1", "text", "平台内核通过插件注册中心加载 RAG 插件。")));

        PluginResult<?> queried = plugin.execute(new PluginRequest(
                "trace-1",
                "user-1",
                "rag.query",
                Map.of("query", "quantum chromodynamics lattice")));

        assertFalse(queried.success());
        assertEquals(RagErrorCodes.NO_EVIDENCE, queried.errorCode());
    }

    @Test
    void missingPayloadFieldIsRejected() {
        RagPlugin plugin = new RagPlugin();
        PluginResult<?> result = plugin.execute(
                new PluginRequest("trace-1", "user-1", "rag.ingest", Map.of("text", "only text")));

        assertFalse(result.success());
        assertEquals(RagErrorCodes.INVALID_PAYLOAD, result.errorCode());
    }

    @Test
    void kernelLoadsSpiPluginAndCompletesClosedLoop() {
        ObservationRegistry observations = ObservationRegistry.create();
        InMemoryPluginRegistry registry = new InMemoryPluginRegistry();
        PluginLifecycle lifecycle = new PluginLifecycle(
                registry, observations, List.of(), new SpiPluginLoader());
        lifecycle.bootstrap();
        PlatformKernel kernel = new PlatformKernel(
                new PreparationOrchestrator(observations, new TaskRouteTable()),
                new ExecutionDispatcher(registry, new PluginExecutor(observations), observations));

        PluginResult<?> ingested = kernel.handle(new PluginRequest(
                "trace-2",
                "user-2",
                "rag.ingest",
                Map.of("docId", "handbook", "text", "故障排查手册写明：队列堆积时先看消费者并发度。")));
        PluginResult<?> queried = kernel.handle(new PluginRequest(
                "trace-2", "user-2", "rag.query", Map.of("query", "队列堆积时看什么")));

        assertTrue(ingested.success());
        assertTrue(queried.success());
        QueryResult result = (QueryResult) queried.data();
        assertTrue(result.answer().contains("消费者并发度"));
    }
}
