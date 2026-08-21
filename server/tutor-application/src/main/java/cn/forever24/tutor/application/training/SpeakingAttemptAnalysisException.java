package cn.forever24.tutor.application.training;

public final class SpeakingAttemptAnalysisException extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public SpeakingAttemptAnalysisException(String code, boolean retryable, String message) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() { return code; }
    public boolean retryable() { return retryable; }
}
