package cn.forever24.tutor.ai.fake;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.ChatProviderRequest;
import cn.forever24.tutor.ai.provider.ChatStream;
import cn.forever24.tutor.ai.provider.JsonSchema;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.StructuredResponse;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static cn.forever24.tutor.ai.provider.ProviderText.requireNonNull;

public class FakeChatProvider implements ChatProvider {

    private static final ProviderCapabilities CAPABILITIES = new ProviderCapabilities(
            FakeProviderVersions.PROVIDER_ID,
            FakeProviderVersions.CHAT_MODEL_ID,
            Set.of(ProviderCapability.CHAT_STREAMING, ProviderCapability.STRUCTURED_OUTPUT),
            Duration.ofSeconds(2)
    );
    private static final String STRUCTURED_CONTENT = """
            {"reply":"This is a deterministic fake coach response.","shouldContinue":true,"nextIntent":"ASK_FOR_CONTEXT","scaffolding":null,"safetyFlags":[]}
            """;
    private static final String CORRECTION_STRUCTURED_CONTENT = """
            {"hasError":false,"corrections":[],"overallFeedback":"Clear and natural enough for this turn."}
            """;

    @Override
    public ChatStream stream(ChatProviderRequest request) {
        return new ChatStream(
                List.of("This is ", "a deterministic ", "fake coach response."),
                trace(request),
                ProviderUsage.freeText(estimateTokens(request.input()), 8)
        );
    }

    @Override
    public StructuredResponse completeStructured(ChatProviderRequest request, JsonSchema schema) {
        JsonSchema safeSchema = requireNonNull(schema, "schema");
        return new StructuredResponse(
                structuredContent(safeSchema),
                trace(request, safeSchema),
                ProviderUsage.freeText(estimateTokens(request.input()), 16),
                false
        );
    }

    @Override
    public ProviderCapabilities capabilities() {
        return CAPABILITIES;
    }

    private ProviderTrace trace(ChatProviderRequest request) {
        return trace(request, null);
    }

    private ProviderTrace trace(ChatProviderRequest request, JsonSchema schema) {
        return new ProviderTrace(
                request.traceId(),
                FakeProviderVersions.PROVIDER_ID,
                FakeProviderVersions.CHAT_MODEL_ID,
                request.promptVersion(),
                schema == null ? request.schemaVersion() : schema.version()
        );
    }

    private String structuredContent(JsonSchema schema) {
        if ("correction-result".equals(schema.name())) {
            return CORRECTION_STRUCTURED_CONTENT.strip();
        }
        return STRUCTURED_CONTENT.strip();
    }

    private int estimateTokens(String input) {
        return Math.max(1, input.trim().split("\\s+").length);
    }
}
