package cn.forever24.tutor.api.quota;

import cn.forever24.tutor.application.quota.DailyQuotaStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record QuotaResponse(
        LocalDate quotaDate,
        int dailyLimit,
        int used,
        int bonus,
        int remaining,
        boolean unlimited,
        OffsetDateTime resetAt
) {

    public static QuotaResponse from(DailyQuotaStatus status) {
        return new QuotaResponse(
                status.quotaDate(),
                status.dailyLimit(),
                status.used(),
                status.bonus(),
                status.remaining(),
                status.unlimited(),
                status.resetAt());
    }
}
