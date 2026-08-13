package cn.forever24.tutor.ai.provider;

import java.math.BigDecimal;
import java.util.Objects;

public record ProviderUsage(
        int inputTokens,
        int outputTokens,
        long audioInputMillis,
        long audioOutputMillis,
        BigDecimal estimatedCostUsd
) {

    public ProviderUsage {
        if (inputTokens < 0 || outputTokens < 0 || audioInputMillis < 0 || audioOutputMillis < 0) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "usage values must not be negative");
        }
        estimatedCostUsd = Objects.requireNonNull(estimatedCostUsd, "estimatedCostUsd must not be null");
        if (estimatedCostUsd.signum() < 0) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "estimatedCostUsd must not be negative");
        }
    }

    public static ProviderUsage freeText(int inputTokens, int outputTokens) {
        return new ProviderUsage(inputTokens, outputTokens, 0, 0, BigDecimal.ZERO);
    }

    public static ProviderUsage freeAudio(long audioInputMillis, long audioOutputMillis) {
        return new ProviderUsage(0, 0, audioInputMillis, audioOutputMillis, BigDecimal.ZERO);
    }
}
