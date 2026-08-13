package cn.forever24.tutor.ai.provider;

import java.util.Map;

import static cn.forever24.tutor.ai.provider.ProviderText.requireNonBlank;

public record JsonSchema(
        String name,
        String version,
        Map<String, Object> document
) {

    public JsonSchema {
        name = requireNonBlank(name, "name");
        version = requireNonBlank(version, "version");
        if (document == null || document.isEmpty()) {
            throw new AiProviderException(AiProviderErrorType.VALIDATION_ERROR, "document must not be empty");
        }
        document = Map.copyOf(document);
    }
}
