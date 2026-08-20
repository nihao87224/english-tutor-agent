package cn.forever24.tutor.curriculum;

public record RetryPolicy(
        boolean criticalErrorRequiresRetry,
        int maximumAttempts,
        boolean retryEvidenceIsIndependent
) {

    public RetryPolicy {
        if (maximumAttempts < 1 || maximumAttempts > 5) {
            throw new IllegalArgumentException("maximumAttempts must be between 1 and 5");
        }
        if (!retryEvidenceIsIndependent) {
            throw new IllegalArgumentException("retry evidence must be independent");
        }
    }
}
