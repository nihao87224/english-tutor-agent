package cn.forever24.tutor.application.auth;

public class AuthException extends RuntimeException {

    private final String code;
    private final int status;

    private AuthException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AuthException badRequest(String code, String message) {
        return new AuthException(code, message, 400);
    }

    public static AuthException conflict(String code, String message) {
        return new AuthException(code, message, 409);
    }

    public static AuthException unauthorized(String code, String message) {
        return new AuthException(code, message, 401);
    }

    public static AuthException invalidCredentials() {
        return unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
