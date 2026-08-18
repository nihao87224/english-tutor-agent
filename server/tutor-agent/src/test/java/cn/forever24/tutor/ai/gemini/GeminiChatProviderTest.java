package cn.forever24.tutor.ai.gemini;

import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiChatProviderTest {

    @Test
    void usesNativeGenerateContentProtocolAndGeminiApiKeyHeader() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/models/gemini-test:generateContent", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            path.set(exchange.getRequestURI().getPath());
            requestBody.set(objectMapper.readTree(exchange.getRequestBody().readAllBytes()));
            byte[] response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"score\\\":8}\"}]}}],\"usageMetadata\":{\"promptTokenCount\":4,\"candidatesTokenCount\":3}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            GeminiChatProvider provider = new GeminiChatProvider(objectMapper, new ActiveAiProviderConfiguration(
                    "gemini", AiProviderType.GEMINI,
                    URI.create("http://localhost:" + server.getAddress().getPort()), "gemini-key", "gemini-test",
                    null, null, null, Duration.ofSeconds(10)));

            var response = provider.completeStructured(
                    ChatProviderRequest.structured("trace-1", "prompt-v1", "schema-v1", "Evaluate this answer."),
                    new JsonSchema("evaluation", "v1", Map.of("type", "object")));

            assertEquals("/models/gemini-test:generateContent", path.get());
            assertEquals("gemini-key", apiKey.get());
            assertEquals("Evaluate this answer.", requestBody.get().path("contents").path(0).path("parts").path(0).path("text").asText());
            assertEquals("application/json", requestBody.get().path("generationConfig").path("responseMimeType").asText());
            assertEquals("{\"score\":8}", response.content());
            assertEquals("gemini", response.trace().providerId());
        } finally {
            server.stop(0);
        }
    }
}
