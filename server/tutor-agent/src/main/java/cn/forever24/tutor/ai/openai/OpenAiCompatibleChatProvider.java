package cn.forever24.tutor.ai.openai;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** OpenAI Chat Completions protocol adapter, used by DeepSeek and similar providers. */
public final class OpenAiCompatibleChatProvider implements ChatProvider {

    private static final int STREAM_CHUNK_SIZE = 8;

    private final OpenAiHttpClient client;
    private final OpenAiProviderProperties properties;
    private final String providerId;

    public OpenAiCompatibleChatProvider(OpenAiHttpClient client, OpenAiProviderProperties properties, String providerId) {
        this.client = client;
        this.properties = properties;
        this.providerId = providerId;
    }

    @Override
    public ChatStream stream(ChatProviderRequest request) {
        JsonNode response = client.postJson("/chat/completions", requestBody(request, null));
        String text = extractText(response);
        return new ChatStream(chunk(text), trace(request), usage(response));
    }

    @Override
    public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
        JsonNode response = client.postJson("/chat/completions", requestBody(request, schema));
        String text = extractText(response);
        validateJson(text);
        return new StructuredResponse(text, trace(request), usage(response), false);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(providerId, properties.llmModel(),
                Set.of(ProviderCapability.CHAT_STREAMING, ProviderCapability.STRUCTURED_OUTPUT), properties.timeout());
    }

    private ObjectNode requestBody(ChatProviderRequest request, JsonSchema schema) {
        ObjectNode body = client.objectMapper().createObjectNode();
        body.put("model", properties.llmModel());
        ObjectNode message = body.putArray("messages").addObject();
        message.put("role", "user");
        message.put("content", schema == null ? request.input() : request.input() + "\nReturn valid JSON only.");
        if (schema != null) {
            body.putObject("response_format").put("type", "json_object");
        }
        return body;
    }

    private String extractText(JsonNode response) {
        String text = response.path("choices").path(0).path("message").path("content").asText("").trim();
        if (text.isBlank()) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, providerId + " response did not contain output text");
        }
        return text;
    }

    private void validateJson(String text) {
        try {
            client.objectMapper().readTree(text);
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, providerId + " structured response was not valid JSON");
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
        return new ProviderTrace(request.traceId(), providerId, properties.llmModel(), request.promptVersion(), request.schemaVersion());
    }

    private ProviderUsage usage(JsonNode response) {
        JsonNode usage = response.path("usage");
        return ProviderUsage.freeText(usage.path("prompt_tokens").asInt(0), usage.path("completion_tokens").asInt(0));
    }
}
