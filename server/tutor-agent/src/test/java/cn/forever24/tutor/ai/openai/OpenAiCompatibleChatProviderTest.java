package cn.forever24.tutor.ai.openai;

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
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiCompatibleChatProviderTest {

    @Test
    void usesChatCompletionsJsonModeForStructuredOutput() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            path.set(exchange.getRequestURI().getPath());
            requestBody.set(objectMapper.readTree(exchange.getRequestBody().readAllBytes()));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"{\\\"score\\\":7}\"}}],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":2}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ActiveAiProviderConfiguration configuration = new ActiveAiProviderConfiguration(
                    "deepseek", AiProviderType.OPENAI_COMPATIBLE,
                    URI.create("http://localhost:" + server.getAddress().getPort()), "deepseek-key", "deepseek-v4-flash",
                    null, null, null, Duration.ofSeconds(10));
            OpenAiProviderProperties properties = OpenAiProviderProperties.fromActiveConfiguration(configuration);
            OpenAiCompatibleChatProvider provider = new OpenAiCompatibleChatProvider(
                    new OpenAiHttpClient(HttpClient.newHttpClient(), objectMapper, properties), properties, "deepseek");

            var response = provider.completeStructured(
                    ChatProviderRequest.structured("trace-1", "prompt-v1", "schema-v1", "Evaluate this answer."),
                    new JsonSchema("evaluation", "v1", Map.of("type", "object")));

            assertEquals("/chat/completions", path.get());
            assertEquals("Bearer deepseek-key", authorization.get());
            assertEquals("deepseek-v4-flash", requestBody.get().path("model").asText());
            assertEquals("json_object", requestBody.get().path("response_format").path("type").asText());
            assertEquals("{\"score\":7}", response.content());
            assertEquals("deepseek", response.trace().providerId());
        } finally {
            server.stop(0);
        }
    }
}
