package cn.forever24.tutor.application.roleplay;

public final class RolePlayResponderException extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public RolePlayResponderException(String code, boolean retryable, String message) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() { return code; }
    public boolean retryable() { return retryable; }
}
