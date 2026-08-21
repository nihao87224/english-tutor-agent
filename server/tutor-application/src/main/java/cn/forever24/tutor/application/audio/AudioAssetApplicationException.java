package cn.forever24.tutor.application.audio;

public final class AudioAssetApplicationException extends RuntimeException {
    private final String code;
    private final int status;

    public AudioAssetApplicationException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public int status() { return status; }

    public static AudioAssetApplicationException idempotencyConflict() {
        return new AudioAssetApplicationException("IDEMPOTENCY_CONFLICT", 409,
                "Idempotency-Key was already used for another audio upload");
    }

    public static AudioAssetApplicationException storageUnavailable() {
        return new AudioAssetApplicationException("AUDIO_STORAGE_UNAVAILABLE", 503,
                "audio storage is temporarily unavailable");
    }
}
