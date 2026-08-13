package cn.forever24.tutor.ai.provider;

public record ProviderTrace(
        String traceId,
        String providerId,
        String modelId,
        String promptVersion,
        String schemaVersion
) {

    public ProviderTrace {
        traceId = ProviderText.requireNonBlank(traceId, "traceId");
        providerId = ProviderText.requireNonBlank(providerId, "providerId");
        modelId = ProviderText.requireNonBlank(modelId, "modelId");
        promptVersion = ProviderText.requireNonBlank(promptVersion, "promptVersion");
        schemaVersion = ProviderText.requireNonBlank(schemaVersion, "schemaVersion");
    }
}
