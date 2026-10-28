package com.llmplatform.plugin.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.model.ModelCompletion;
import com.llmplatform.core.model.ModelGateway;
import com.llmplatform.core.model.ModelRequest;
import com.llmplatform.core.model.ModelStreamListener;
import com.llmplatform.core.model.ModelUnavailableException;
import com.llmplatform.core.plugin.DefaultPluginContext;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.plugin.rag.answer.QueryResult;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RagModelAnswerTest {

    @Test
    void queryAsksGatewayWithEvidenceAndReturnsModelText() {
        List<String> prompts = new ArrayList<>();
        RagPlugin plugin = plugin(new FixedGateway(prompts, new ModelCompletion("remote-openai", "先看消费者并发度")));

        plugin.execute(ingest());
        PluginResult<?> result = plugin.execute(query());

        assertTrue(result.success());
        QueryResult data = (QueryResult) result.data();
        assertEquals("先看消费者并发度", data.answer());
        assertEquals("remote-openai", data.modelId());
        assertFalse(data.answer().startsWith("根据检索到的资料"));
        assertTrue(prompts.get(0).contains("消费者并发度"));
        assertTrue(prompts.get(0).contains("问题："));
    }

    @Test
    void modelOutageDoesNotPretendToAnswer() {
        RagPlugin plugin = plugin(new FixedGateway(new ArrayList<>(), null));
        plugin.execute(ingest());

        PluginResult<?> result = plugin.execute(query());

        assertFalse(result.success());
        assertEquals(RagErrorCodes.MODEL_UNAVAILABLE, result.errorCode());
    }

    private static RagPlugin plugin(ModelGateway gateway) {
        RagPlugin plugin = new RagPlugin();
        plugin.init(new DefaultPluginContext("trace-1", "user-1", ObservationRegistry.create(), gateway));
        return plugin;
    }

    private static PluginRequest ingest() {
        return new PluginRequest("trace-1", "user-1", "rag.ingest", Map.of(
                "docId", "handbook",
                "text", "故障排查手册写明：队列堆积时先看消费者并发度。"));
    }

    private static PluginRequest query() {
        return new PluginRequest("trace-1", "user-1", "rag.query", Map.of("query", "队列堆积时看什么"));
    }

    private static final class FixedGateway implements ModelGateway {
        private final List<String> prompts;
        private final ModelCompletion completion;

        private FixedGateway(List<String> prompts, ModelCompletion completion) {
            this.prompts = prompts;
            this.completion = completion;
        }

        @Override
        public ModelCompletion complete(ModelRequest request) {
            prompts.add(request.prompt());
            if (completion == null) {
                throw new ModelUnavailableException("all down");
            }
            return completion;
        }

        @Override
        public void stream(ModelRequest request, ModelStreamListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
