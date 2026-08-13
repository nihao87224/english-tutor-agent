package cn.forever24.tutor.ai.provider;

import java.util.List;
import java.util.Objects;

public record ChatStream(
        List<String> chunks,
        ProviderTrace trace,
        ProviderUsage usage
) {

    public ChatStream {
        chunks = List.copyOf(Objects.requireNonNull(chunks, "chunks must not be null"));
        if (chunks.isEmpty() || chunks.stream().anyMatch(chunk -> chunk == null || chunk.isBlank())) {
            throw new AiProviderException(AiProviderErrorType.INVALID_OUTPUT, "stream chunks must not be blank");
        }
        trace = Objects.requireNonNull(trace, "trace must not be null");
        usage = Objects.requireNonNull(usage, "usage must not be null");
    }
}
