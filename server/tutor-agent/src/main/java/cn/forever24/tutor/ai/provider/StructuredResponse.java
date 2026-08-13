package cn.forever24.tutor.ai.provider;

import java.util.Objects;

public record StructuredResponse(
        String content,
        ProviderTrace trace,
        ProviderUsage usage,
        boolean repaired
) {

    public StructuredResponse {
        content = ProviderText.requireNonBlank(content, "content");
        trace = Objects.requireNonNull(trace, "trace must not be null");
        usage = Objects.requireNonNull(usage, "usage must not be null");
    }
}
