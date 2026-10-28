package com.llmplatform.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llmplatform.core.plugin.PluginResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

final class JsonResponses {

    private JsonResponses() {
    }

    static void write(HttpServletResponse response, int status, String errorCode, String message, ObjectMapper objectMapper)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), PluginResult.failure(errorCode, message));
    }
}
