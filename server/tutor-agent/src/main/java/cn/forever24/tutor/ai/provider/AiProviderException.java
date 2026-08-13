package cn.forever24.tutor.ai.provider;

import java.util.Objects;

public class AiProviderException extends RuntimeException {

    private final AiProviderErrorType errorType;

    public AiProviderException(AiProviderErrorType errorType, String message) {
        super(message);
        this.errorType = Objects.requireNonNull(errorType, "errorType must not be null");
    }

    public AiProviderErrorType errorType() {
        return errorType;
    }
}
