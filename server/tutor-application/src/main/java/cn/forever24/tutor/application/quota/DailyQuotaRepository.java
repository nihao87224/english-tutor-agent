package cn.forever24.tutor.application.quota;

import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface DailyQuotaRepository {

    DailyQuotaStatus getStatus(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaPolicy policy,
            OffsetDateTime resetAt);

    QuotaReservation reserve(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaRequestType requestType,
            String idempotencyKey,
            QuotaPolicy policy,
            Instant now,
            Instant expiresAt,
            OffsetDateTime resetAt);

    void commit(String reservationId, Instant now);

    void refund(String reservationId, Instant now);

    void refundStaleReservations(Instant now);
}
