package com.llmplatform.gateway.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.core.model.ModelUnavailableException;
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

class OllamaChatModelTest {

    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"message\":{\"content\":\"你好\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/api/tags", exchange -> {
            byte[] body = "{\"models\":[]}".getBytes(StandardCharsets.UTF_8);
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
    void completePostsChatRequestAndReadsContent() throws Exception {
        OllamaChatModel model = model();

        assertEquals("你好", model.complete("讲一下插件"));
        JsonNode body = mapper.readTree(lastBody.get());
        assertEquals("qwen3:8b", body.path("model").asText());
        assertEquals("讲一下插件", body.path("messages").path(0).path("content").asText());
        assertEquals(false, body.path("stream").asBoolean());
    }

    @Test
    void probeAcceptsTagsEndpoint() {
        model().probe();
    }

    @Test
    void nonSuccessStatusIsUnavailable() {
        server.removeContext("/api/chat");
        server.createContext("/api/chat", exchange -> {
            byte[] body = "no".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });

        assertThrows(ModelUnavailableException.class, () -> model().complete("x"));
    }

    @Test
    void streamReadsContentDeltas() {
        server.removeContext("/api/chat");
        server.createContext("/api/chat", exchange -> {
            byte[] body = """
                    {"message":{"content":"你"},"done":false}
                    {"message":{"content":"好"},"done":true}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        List<String> deltas = new ArrayList<>();

        model().stream("hi", deltas::add);

        assertEquals(List.of("你", "好"), deltas);
        assertTrue(deltas.size() == 2);
    }

    private OllamaChatModel model() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new OllamaChatModel("ollama-local", baseUrl, "qwen3:8b", Duration.ofSeconds(2));
    }
}
