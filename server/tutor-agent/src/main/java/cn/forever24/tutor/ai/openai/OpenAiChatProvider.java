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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OpenAiChatProvider implements ChatProvider {

    private static final int STREAM_CHUNK_SIZE = 8;

    private final OpenAiHttpClient client;
    private final OpenAiProviderProperties properties;

    public OpenAiChatProvider(OpenAiHttpClient client, OpenAiProviderProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public ChatStream stream(ChatProviderRequest request) {
        JsonNode response = client.postJson("/responses", responseBody(request));
        String text = extractOutputText(response);
        return new ChatStream(
                chunk(text),
                trace(request),
                usage(response, 0, 0));
    }

    @Override
    public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
        JsonNode response = client.postJson("/responses", structuredBody(request, schema));
        String text = extractOutputText(response);
        validateJson(text);
        return new StructuredResponse(
                text,
                trace(request),
                usage(response, 0, 0),
                false);
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                OpenAiProviderProperties.PROVIDER_ID,
                properties.llmModel(),
                Set.of(ProviderCapability.CHAT_STREAMING, ProviderCapability.STRUCTURED_OUTPUT),
                properties.timeout());
    }

    private ObjectNode responseBody(ChatProviderRequest request) {
        ObjectNode body = client.objectMapper().createObjectNode();
        body.put("model", properties.llmModel());
        body.put("input", request.input());
        body.putObject("text").put("verbosity", "low");
        return body;
    }

    private ObjectNode structuredBody(ChatProviderRequest request, JsonSchema schema) {
        ObjectNode body = responseBody(request);
        ObjectNode text = (ObjectNode) body.get("text");
        ObjectNode format = client.objectMapper().createObjectNode();
        format.put("type", "json_schema");
        format.put("name", schema.name());
        format.set("schema", client.objectMapper().valueToTree(schema.document()));
        format.put("strict", true);
        text.set("format", format);
        return body;
    }

    private String extractOutputText(JsonNode response) {
        JsonNode direct = response.get("output_text");
        if (direct != null && direct.isTextual() && !direct.asText().isBlank()) {
            return direct.asText();
        }
        List<String> texts = new ArrayList<>();
        collectOutputText(response, texts);
        String text = String.join("", texts).trim();
        if (text.isBlank()) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "OpenAI response did not contain output text");
        }
        return text;
    }

    private void collectOutputText(JsonNode node, List<String> texts) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            JsonNode text = node.get("text");
            if (type != null && "output_text".equals(type.asText()) && text != null && text.isTextual()) {
                texts.add(text.asText());
                return;
            }
            node.fields().forEachRemaining(entry -> collectOutputText(entry.getValue(), texts));
        } else if (node.isArray()) {
            node.forEach(child -> collectOutputText(child, texts));
        }
    }

    private void validateJson(String text) {
        try {
            client.objectMapper().readTree(text);
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "OpenAI structured response was not valid JSON");
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
        return new ProviderTrace(
                request.traceId(),
                OpenAiProviderProperties.PROVIDER_ID,
                properties.llmModel(),
                request.promptVersion(),
                request.schemaVersion());
    }

    private ProviderUsage usage(JsonNode response, long audioInputMillis, long audioOutputMillis) {
        JsonNode usage = response.get("usage");
        int inputTokens = usage == null ? 0 : usage.path("input_tokens").asInt(0);
        int outputTokens = usage == null ? 0 : usage.path("output_tokens").asInt(0);
        return new ProviderUsage(inputTokens, outputTokens, audioInputMillis, audioOutputMillis, java.math.BigDecimal.ZERO);
    }
}
