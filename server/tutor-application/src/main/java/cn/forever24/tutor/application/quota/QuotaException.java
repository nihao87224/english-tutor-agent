package cn.forever24.tutor.application.quota;

public class QuotaException extends RuntimeException {

    private final String code;
    private final int status;
    private final DailyQuotaStatus quotaStatus;

    private QuotaException(String code, String message, int status, DailyQuotaStatus quotaStatus) {
        super(message);
        this.code = code;
        this.status = status;
        this.quotaStatus = quotaStatus;
    }

    public static QuotaException exceeded(DailyQuotaStatus quotaStatus) {
        return new QuotaException(
                "DAILY_QUOTA_EXCEEDED",
                "Daily AI request quota has been exhausted",
                429,
                quotaStatus);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public DailyQuotaStatus quotaStatus() {
        return quotaStatus;
    }
}
