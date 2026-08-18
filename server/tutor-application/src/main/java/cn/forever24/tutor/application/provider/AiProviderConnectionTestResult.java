package cn.forever24.tutor.application.provider;

public record AiProviderConnectionTestResult(boolean success, long latencyMs, String error) {

    public AiProviderConnectionTestResult {
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
        if (success && error != null) {
            throw new IllegalArgumentException("successful connection tests cannot contain an error");
        }
        if (!success && (error == null || error.isBlank())) {
            throw new IllegalArgumentException("failed connection tests require an error");
        }
    }

    public static AiProviderConnectionTestResult success(long latencyMs) {
        return new AiProviderConnectionTestResult(true, latencyMs, null);
    }

    public static AiProviderConnectionTestResult failure(long latencyMs, String error) {
        return new AiProviderConnectionTestResult(false, latencyMs, error);
    }
}
