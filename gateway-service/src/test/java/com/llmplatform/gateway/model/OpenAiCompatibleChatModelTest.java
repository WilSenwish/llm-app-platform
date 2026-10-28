package com.llmplatform.gateway.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleChatModelTest {

    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            boolean stream = mapper.readTree(lastBody.get()).path("stream").asBoolean();
            byte[] body = stream
                    ? """
                    data: {"choices":[{"delta":{"content":"降"}}]}

                    data: {"choices":[{"delta":{"content":"级"}}]}

                    data: [DONE]
                    """.getBytes(StandardCharsets.UTF_8)
                    : "{\"choices\":[{\"message\":{\"content\":\"远端回答\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/v1/models", exchange -> {
            byte[] body = "{\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void completeSendsBearerTokenAndReadsMessage() throws Exception {
        OpenAiCompatibleChatModel model = model();

        assertEquals("远端回答", model.complete("问题"));
        JsonNode body = mapper.readTree(lastBody.get());
        assertEquals("qwen", body.path("model").asText());
        assertEquals("问题", body.path("messages").path(0).path("content").asText());
        assertEquals("Bearer secret", authorization.get());
    }

    @Test
    void streamParsesSseDeltas() {
        List<String> deltas = new ArrayList<>();

        model().stream("问题", deltas::add);

        assertEquals(List.of("降", "级"), deltas);
        assertTrue(authorization.get().startsWith("Bearer "));
    }

    private OpenAiCompatibleChatModel model() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new OpenAiCompatibleChatModel("remote-openai", baseUrl, "qwen", "secret", Duration.ofSeconds(2));
    }
}
