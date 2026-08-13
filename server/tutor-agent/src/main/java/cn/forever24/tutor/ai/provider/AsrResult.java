package cn.forever24.tutor.ai.provider;

import java.util.Objects;

public record AsrResult(
        String transcript,
        double confidence,
        ProviderTrace trace,
        ProviderUsage usage
) {

    public AsrResult {
        transcript = ProviderText.requireNonBlank(transcript, "transcript");
        if (confidence < 0 || confidence > 1) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "confidence must be between 0 and 1");
        }
        trace = Objects.requireNonNull(trace, "trace must not be null");
        usage = Objects.requireNonNull(usage, "usage must not be null");
    }
}
