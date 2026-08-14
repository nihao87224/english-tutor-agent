package cn.forever24.tutor.application.quota;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DailyQuotaStatus(
        LocalDate quotaDate,
        int dailyLimit,
        int used,
        int bonus,
        int remaining,
        boolean unlimited,
        OffsetDateTime resetAt
) {
}
