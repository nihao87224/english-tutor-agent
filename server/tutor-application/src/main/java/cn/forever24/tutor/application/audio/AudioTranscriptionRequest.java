package cn.forever24.tutor.application.audio;

import java.time.Duration;
import java.util.Arrays;

public record AudioTranscriptionRequest(
        String traceId,
        byte[] content,
        String mimeType,
        Duration duration
) {
    public AudioTranscriptionRequest {
        if (traceId == null || traceId.isBlank() || mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("traceId and mimeType are required");
        }
        if (content == null || content.length == 0 || duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("audio content and positive duration are required");
        }
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
