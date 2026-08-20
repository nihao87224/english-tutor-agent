package cn.forever24.tutor.application.planning;

public final class PrescriptionApplicationException extends RuntimeException {

    private final String code;
    private final int status;
    private final Boolean fallbackAvailable;

    public PrescriptionApplicationException(String code, int status, String message) {
        this(code, status, message, null);
    }

    public PrescriptionApplicationException(String code, int status, String message, Boolean fallbackAvailable) {
        super(message);
        this.code = code;
        this.status = status;
        this.fallbackAvailable = fallbackAvailable;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public Boolean fallbackAvailable() {
        return fallbackAvailable;
    }

    public static PrescriptionApplicationException noCandidate(boolean fallbackAvailable) {
        return new PrescriptionApplicationException(
                "PRESCRIPTION_NO_CANDIDATE", 409,
                "no accessible prescription candidate satisfies the teaching constraints",
                fallbackAvailable);
    }
}
