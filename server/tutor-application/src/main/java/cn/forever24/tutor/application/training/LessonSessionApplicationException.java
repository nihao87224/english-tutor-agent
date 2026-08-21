package cn.forever24.tutor.application.training;

public final class LessonSessionApplicationException extends RuntimeException {

    private final String code;
    private final int status;

    public LessonSessionApplicationException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public static LessonSessionApplicationException notFound() {
        return new LessonSessionApplicationException("SESSION_NOT_FOUND", 404, "lesson session was not found");
    }

    public static LessonSessionApplicationException stalePrescription() {
        return new LessonSessionApplicationException(
                "PRESCRIPTION_STALE", 409, "prescription or block is no longer active");
    }

    public static LessonSessionApplicationException stateConflict(String message) {
        return new LessonSessionApplicationException("SESSION_STATE_CONFLICT", 409, message);
    }

    public static LessonSessionApplicationException versionConflict() {
        return new LessonSessionApplicationException(
                "VERSION_CONFLICT", 409, "lesson session changed during this request");
    }

    public static LessonSessionApplicationException attemptNotFound() {
        return new LessonSessionApplicationException("ATTEMPT_NOT_FOUND", 404, "lesson attempt was not found");
    }

    public static LessonSessionApplicationException idempotencyConflict(String operation) {
        return new LessonSessionApplicationException(
                "IDEMPOTENCY_CONFLICT", 409, "Idempotency-Key was already used for another " + operation);
    }

    public static LessonSessionApplicationException audioAssetNotFound() {
        return new LessonSessionApplicationException(
                "AUDIO_ASSET_NOT_FOUND", 404, "owned ready audio asset was not found");
    }

    public static LessonSessionApplicationException transcriptConfirmationRequired() {
        return new LessonSessionApplicationException(
                "ASR_CONFIRMATION_REQUIRED", 409, "the transcript must be confirmed, corrected or re-recorded");
    }
}
