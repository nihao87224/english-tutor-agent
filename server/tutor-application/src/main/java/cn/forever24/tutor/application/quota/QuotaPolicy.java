package cn.forever24.tutor.application.quota;

public record QuotaPolicy(int dailyLimit, boolean unlimited) {

    public QuotaPolicy {
        if (dailyLimit < 0) {
            throw new IllegalArgumentException("daily limit must not be negative");
        }
    }
}
