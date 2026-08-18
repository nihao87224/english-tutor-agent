package cn.forever24.tutor.ai.gemini;

import cn.forever24.tutor.ai.provider.AiProviderErrorType;
import cn.forever24.tutor.ai.provider.AiProviderException;
import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.StructuredResponse;
import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Native Gemini generateContent protocol adapter. */
public final class GeminiChatProvider implements ChatProvider {

    private static final int STREAM_CHUNK_SIZE = 8;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ActiveAiProviderConfiguration configuration;

    public GeminiChatProvider(ObjectMapper objectMapper, ActiveAiProviderConfiguration configuration) {
        this(HttpClient.newBuilder().connectTimeout(configuration.timeout()).build(), objectMapper, configuration);
    }

    GeminiChatProvider(HttpClient httpClient, ObjectMapper objectMapper, ActiveAiProviderConfiguration configuration) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    @Override
    public ChatStream stream(ChatProviderRequest request) {
        JsonNode response = generate(request, null);
        String text = extractText(response);
        return new ChatStream(chunk(text), trace(request), usage(response));
    }

    @Override
    public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
        JsonNode response = generate(request, schema);
        String text = extractText(response);
        validateJson(text);
        return new StructuredResponse(text, trace(request), usage(response), false);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(configuration.providerCode(), requireLlmModel(),
                Set.of(ProviderCapability.CHAT_STREAMING, ProviderCapability.STRUCTURED_OUTPUT), configuration.timeout());
    }

    private JsonNode generate(ChatProviderRequest request, JsonSchema schema) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("contents").addObject().put("role", "user").putArray("parts").addObject().put("text", request.input());
        if (schema != null) {
            ObjectNode generationConfig = body.putObject("generationConfig");
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.set("responseJsonSchema", objectMapper.valueToTree(schema.document()));
        }
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint())
                    .timeout(configuration.timeout())
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", configuration.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerException(response.statusCode(), response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(AiProviderErrorType.TIMEOUT, "Gemini request was interrupted");
        } catch (IOException exception) {
            throw new AiProviderException(AiProviderErrorType.PROVIDER_UNAVAILABLE, "Gemini request failed: " + exception.getMessage());
        }
    }

    private URI endpoint() {
        URI baseUrl = configuration.baseUrl();
        String path = baseUrl.getPath() + "/models/" + requireLlmModel() + ":generateContent";
        return baseUrl.resolve(path);
    }

    private String extractText(JsonNode response) {
        String text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("").trim();
        if (text.isBlank()) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "Gemini response did not contain output text");
        }
        return text;
    }

    private void validateJson(String text) {
        try {
            objectMapper.readTree(text);
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "Gemini structured response was not valid JSON");
        }
    }

    private List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        for (int index = 0; index < text.length(); index += STREAM_CHUNK_SIZE) {
            chunks.add(text.substring(index, Math.min(text.length(), index + STREAM_CHUNK_SIZE)));
        }
        return chunks;
    }

    private ProviderTrace trace(ChatProviderRequest request) {
        return new ProviderTrace(request.traceId(), configuration.providerCode(), requireLlmModel(), request.promptVersion(), request.schemaVersion());
    }

    private ProviderUsage usage(JsonNode response) {
        JsonNode usage = response.path("usageMetadata");
        return ProviderUsage.freeText(usage.path("promptTokenCount").asInt(0), usage.path("candidatesTokenCount").asInt(0));
    }

    private String requireLlmModel() {
        if (configuration.llmModel() == null) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "llmModel must be configured for Gemini");
        }
        return configuration.llmModel();
    }

    private AiProviderException providerException(int statusCode, String responseBody) {
        AiProviderErrorType type = statusCode == 401 || statusCode == 403
                ? AiProviderErrorType.AUTHENTICATION_FAILED
                : statusCode == 408 || statusCode == 429 || statusCode >= 500
                ? AiProviderErrorType.PROVIDER_UNAVAILABLE
                : AiProviderErrorType.INVALID_OUTPUT;
        String body = responseBody == null || responseBody.isBlank() ? "empty response body" : responseBody.substring(0, Math.min(500, responseBody.length()));
        return new AiProviderException(type, "Gemini returned HTTP " + statusCode + ": " + body);
    }
}
