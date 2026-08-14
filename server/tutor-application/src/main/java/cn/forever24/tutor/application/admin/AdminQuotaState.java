package cn.forever24.tutor.application.admin;

import java.time.LocalDate;

public record AdminQuotaState(
        String userKey,
        Integer dailyLimitOverride,
        boolean unlimited,
        LocalDate quotaDate,
        int dailyLimit,
        int used,
        int reserved,
        int bonus,
        int remaining
) {
}
