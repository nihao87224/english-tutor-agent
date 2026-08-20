package cn.forever24.tutor.application.entitlement;

public final class EntitlementApplicationException extends RuntimeException {

    private final String code;
    private final int status;

    private EntitlementApplicationException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static EntitlementApplicationException forbidden(String code, String message) {
        return new EntitlementApplicationException(code, message, 403);
    }

    public static EntitlementApplicationException notFound(String code, String message) {
        return new EntitlementApplicationException(code, message, 404);
    }

    public static EntitlementApplicationException conflict(String code, String message) {
        return new EntitlementApplicationException(code, message, 409);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
