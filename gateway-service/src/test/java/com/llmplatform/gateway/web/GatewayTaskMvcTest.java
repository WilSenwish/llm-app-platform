package com.llmplatform.gateway.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.llmplatform.core.model.ModelUnavailableException;
import com.llmplatform.gateway.GatewayApplication;
import com.llmplatform.gateway.model.ModelCatalog;
import com.llmplatform.gateway.support.ScriptedChatModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(GatewayTaskMvcTest.FakeModels.class)
@TestPropertySource(properties = {
        "llm.gateway.api-key=test-key",
        "llm.gateway.global-permits-per-second=100",
        "llm.gateway.user-permits-per-second=100",
        "llm.gateway.max-attempts=1"
})
class GatewayTaskMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ragQueryFailsOverThroughGatewayKernelAndPlugin() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyFilter.HEADER, "test-key")
                        .header(RateLimitFilter.USER_HEADER, "user-1")
                        .content("""
                                {"traceId":"t-in","userId":"user-1","taskType":"rag.ingest","payload":{"docId":"handbook","text":"故障排查手册写明：队列堆积时先看消费者并发度。"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApiKeyFilter.HEADER, "test-key")
                        .header(RateLimitFilter.USER_HEADER, "user-1")
                        .content("""
                                {"traceId":"t-q","userId":"user-1","taskType":"rag.query","payload":{"query":"队列堆积时看什么"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("网关降级后的回答：先看消费者并发度"))
                .andExpect(jsonPath("$.data.modelId").value("remote-openai"));
    }

    @Test
    void missingApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-1\",\"taskType\":\"rag.query\",\"payload\":{\"query\":\"x\"}}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @TestConfiguration
    static class FakeModels {
        @Bean
        @Primary
        ModelCatalog modelCatalog() {
            ScriptedChatModel primary = new ScriptedChatModel("ollama-local", () -> {
                throw new ModelUnavailableException("ollama down");
            }, List.of());
            ScriptedChatModel fallback = new ScriptedChatModel(
                    "remote-openai", () -> "网关降级后的回答：先看消费者并发度", List.of("降", "级"));
            return () -> List.of(primary, fallback);
        }
    }
}
