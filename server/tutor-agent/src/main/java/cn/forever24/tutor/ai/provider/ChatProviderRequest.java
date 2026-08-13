package cn.forever24.tutor.ai.provider;

import java.util.Map;
import java.util.Objects;

public record ChatProviderRequest(
        String traceId,
        String promptVersion,
        String schemaVersion,
        String input,
        Map<String, String> metadata
) {

    public ChatProviderRequest {
        traceId = ProviderText.requireNonBlank(traceId, "traceId");
        promptVersion = ProviderText.requireNonBlank(promptVersion, "promptVersion");
        schemaVersion = ProviderText.requireNonBlank(schemaVersion, "schemaVersion");
        input = ProviderText.requireNonBlank(input, "input");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    public static ChatProviderRequest structured(String traceId, String promptVersion, String schemaVersion, String input) {
        return new ChatProviderRequest(traceId, promptVersion, schemaVersion, input, Map.of());
    }
}
