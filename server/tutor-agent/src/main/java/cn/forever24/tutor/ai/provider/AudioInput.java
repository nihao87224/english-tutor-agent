package cn.forever24.tutor.ai.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

public record AudioInput(
        String traceId,
        byte[] content,
        String contentType,
        Duration duration
) {

    public AudioInput {
        traceId = ProviderText.requireNonBlank(traceId, "traceId");
        Objects.requireNonNull(content, "content must not be null");
        if (content.length == 0) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "content must not be empty");
        }
        content = Arrays.copyOf(content, content.length);
        contentType = ProviderText.requireNonBlank(contentType, "contentType");
        duration = Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "duration must be positive");
        }
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
