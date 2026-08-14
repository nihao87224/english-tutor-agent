package cn.forever24.tutor.application.quota;

import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class TestDailyQuotaRepository implements DailyQuotaRepository {

    private final AtomicInteger sequence = new AtomicInteger(0);
    private final AtomicInteger commitCount = new AtomicInteger(0);
    private final AtomicInteger refundCount = new AtomicInteger(0);

    @Override
    public DailyQuotaStatus getStatus(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaPolicy policy,
            OffsetDateTime resetAt
    ) {
        return new DailyQuotaStatus(quotaDate, policy.dailyLimit(), 0, 0, policy.dailyLimit(), policy.unlimited(), resetAt);
    }

    @Override
    public QuotaReservation reserve(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaRequestType requestType,
            String idempotencyKey,
            QuotaPolicy policy,
            Instant now,
            Instant expiresAt,
            OffsetDateTime resetAt
    ) {
        return new QuotaReservation(
                "quota-test-" + sequence.incrementAndGet(),
                userKey.value(),
                quotaDate,
                requestType,
                idempotencyKey,
                QuotaReservationStatus.RESERVED);
    }

    @Override
    public void commit(String reservationId, Instant now) {
        commitCount.incrementAndGet();
    }

    @Override
    public void refund(String reservationId, Instant now) {
        refundCount.incrementAndGet();
    }

    @Override
    public void refundStaleReservations(Instant now) {
    }

    public int commitCount() {
        return commitCount.get();
    }

    public int refundCount() {
        return refundCount.get();
    }
}
