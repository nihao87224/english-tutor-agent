package cn.forever24.tutor.application.quota;

import cn.forever24.tutor.profile.UserKey;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class DailyQuotaApplicationService {

    private static final int RESERVATION_TTL_SECONDS = 300;

    private final DailyQuotaRepository dailyQuotaRepository;
    private final Clock clock;
    private final int defaultDailyLimit;
    private final ZoneId resetZone;

    public DailyQuotaApplicationService(
            DailyQuotaRepository dailyQuotaRepository,
            Clock clock,
            int defaultDailyLimit,
            ZoneId resetZone
    ) {
        if (defaultDailyLimit < 0) {
            throw new IllegalArgumentException("default daily limit must not be negative");
        }
        this.dailyQuotaRepository = dailyQuotaRepository;
        this.clock = clock;
        this.defaultDailyLimit = defaultDailyLimit;
        this.resetZone = resetZone;
    }

    public DailyQuotaStatus currentQuota(String userKeyValue) {
        UserKey userKey = new UserKey(userKeyValue);
        Instant now = clock.instant();
        LocalDate quotaDate = quotaDate(now);
        return dailyQuotaRepository.getStatus(
                userKey,
                quotaDate,
                defaultPolicy(),
                resetAt(quotaDate));
    }

    public QuotaReservation reserve(String userKeyValue, QuotaRequestType requestType, String idempotencyKey) {
        UserKey userKey = new UserKey(userKeyValue);
        if (requestType == null) {
            throw new IllegalArgumentException("quota request type is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        Instant now = clock.instant();
        dailyQuotaRepository.refundStaleReservations(now);
        return dailyQuotaRepository.reserve(
                userKey,
                quotaDate(now),
                requestType,
                idempotencyKey.trim(),
                defaultPolicy(),
                now,
                now.plusSeconds(RESERVATION_TTL_SECONDS),
                resetAt(quotaDate(now)));
    }

    public void commit(QuotaReservation reservation) {
        if (reservation != null && reservation.status() == QuotaReservationStatus.RESERVED) {
            dailyQuotaRepository.commit(reservation.id(), clock.instant());
        }
    }

    public void refund(QuotaReservation reservation) {
        if (reservation != null && reservation.status() == QuotaReservationStatus.RESERVED) {
            dailyQuotaRepository.refund(reservation.id(), clock.instant());
        }
    }

    private QuotaPolicy defaultPolicy() {
        return new QuotaPolicy(defaultDailyLimit, false);
    }

    private LocalDate quotaDate(Instant now) {
        return now.atZone(resetZone).toLocalDate();
    }

    private OffsetDateTime resetAt(LocalDate quotaDate) {
        return quotaDate.plusDays(1).atStartOfDay(resetZone).toOffsetDateTime();
    }
}
