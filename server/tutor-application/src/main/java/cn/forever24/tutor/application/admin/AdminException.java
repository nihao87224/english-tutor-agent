package cn.forever24.tutor.application.admin;

public class AdminException extends RuntimeException {

    private final String code;
    private final int status;

    private AdminException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AdminException notFound(String message) {
        return new AdminException("ADMIN_RESOURCE_NOT_FOUND", message, 404);
    }

    public static AdminException invalid(String message) {
        return new AdminException("INVALID_ADMIN_REQUEST", message, 400);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
