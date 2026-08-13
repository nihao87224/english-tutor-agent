package cn.forever24.tutor.ai.provider;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

public record TtsResult(
        byte[] audio,
        String contentType,
        Duration duration,
        ProviderTrace trace,
        ProviderUsage usage
) {

    public TtsResult {
        Objects.requireNonNull(audio, "audio must not be null");
        if (audio.length == 0) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "audio must not be empty");
        }
        audio = Arrays.copyOf(audio, audio.length);
        contentType = ProviderText.requireNonBlank(contentType, "contentType");
        duration = Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "duration must be positive");
        }
        trace = Objects.requireNonNull(trace, "trace must not be null");
        usage = Objects.requireNonNull(usage, "usage must not be null");
    }

    @Override
    public byte[] audio() {
        return Arrays.copyOf(audio, audio.length);
    }
}
