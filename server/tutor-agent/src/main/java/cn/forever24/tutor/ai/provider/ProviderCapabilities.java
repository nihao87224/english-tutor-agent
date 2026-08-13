package cn.forever24.tutor.ai.provider;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public record ProviderCapabilities(
        String providerId,
        String modelId,
        Set<ProviderCapability> capabilities,
        Duration timeout
) {

    public ProviderCapabilities {
        providerId = ProviderText.requireNonBlank(providerId, "providerId");
        modelId = ProviderText.requireNonBlank(modelId, "modelId");
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities must not be null"));
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "timeout must be positive");
        }
    }
}
