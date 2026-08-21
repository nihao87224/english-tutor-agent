package cn.forever24.tutor.application.audio;

public final class AudioTranscriptionException extends RuntimeException {
    private final boolean retryable;

    public AudioTranscriptionException(boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
