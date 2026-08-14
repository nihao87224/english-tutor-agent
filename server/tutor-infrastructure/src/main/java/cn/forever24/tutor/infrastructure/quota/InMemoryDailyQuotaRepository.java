package cn.forever24.tutor.infrastructure.quota;

import cn.forever24.tutor.application.quota.DailyQuotaRepository;
import cn.forever24.tutor.application.quota.DailyQuotaStatus;
import cn.forever24.tutor.application.quota.QuotaException;
import cn.forever24.tutor.application.quota.QuotaPolicy;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import cn.forever24.tutor.application.quota.QuotaReservationStatus;
import cn.forever24.tutor.profile.UserKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryDailyQuotaRepository implements DailyQuotaRepository {

    private final Map<String, Usage> usageByUserAndDate = new HashMap<>();
    private final Map<String, StoredReservation> reservationsById = new HashMap<>();
    private final Map<String, String> reservationIdByIdempotency = new HashMap<>();

    @Override
    public synchronized DailyQuotaStatus getStatus(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaPolicy policy,
            OffsetDateTime resetAt
    ) {
        Usage usage = usageFor(userKey, quotaDate, policy);
        return status(quotaDate, usage, resetAt);
    }

    @Override
    public synchronized QuotaReservation reserve(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaRequestType requestType,
            String idempotencyKey,
            QuotaPolicy policy,
            Instant now,
            Instant expiresAt,
            OffsetDateTime resetAt
    ) {
        String idempotencyScope = idempotencyScope(userKey, requestType, idempotencyKey);
        String existingId = reservationIdByIdempotency.get(idempotencyScope);
        if (existingId != null) {
            return reservationsById.get(existingId).toReservation();
        }

        Usage usage = usageFor(userKey, quotaDate, policy);
        if (!usage.unlimited && usage.used + usage.reserved >= usage.dailyLimit + usage.bonus) {
            throw QuotaException.exceeded(status(quotaDate, usage, resetAt));
        }

        usage.reserved++;
        String reservationId = "quota-" + UUID.randomUUID();
        StoredReservation reservation = new StoredReservation(
                reservationId,
                userKey.value(),
                quotaDate,
                requestType,
                idempotencyKey,
                QuotaReservationStatus.RESERVED,
                expiresAt);
        reservationsById.put(reservationId, reservation);
        reservationIdByIdempotency.put(idempotencyScope, reservationId);
        return reservation.toReservation();
    }

    @Override
    public synchronized void commit(String reservationId, Instant now) {
        StoredReservation reservation = reservationsById.get(reservationId);
        if (reservation == null || reservation.status != QuotaReservationStatus.RESERVED) {
            return;
        }
        Usage usage = usageByUserAndDate.get(usageKey(reservation.userKey, reservation.quotaDate));
        if (usage != null) {
            usage.reserved = Math.max(0, usage.reserved - 1);
            usage.used++;
        }
        reservation.status = QuotaReservationStatus.COMMITTED;
    }

    @Override
    public synchronized void refund(String reservationId, Instant now) {
        StoredReservation reservation = reservationsById.get(reservationId);
        if (reservation == null || reservation.status != QuotaReservationStatus.RESERVED) {
            return;
        }
        Usage usage = usageByUserAndDate.get(usageKey(reservation.userKey, reservation.quotaDate));
        if (usage != null) {
            usage.reserved = Math.max(0, usage.reserved - 1);
        }
        reservation.status = QuotaReservationStatus.REFUNDED;
    }

    @Override
    public synchronized void refundStaleReservations(Instant now) {
        for (StoredReservation reservation : reservationsById.values()) {
            if (reservation.status == QuotaReservationStatus.RESERVED && !reservation.expiresAt.isAfter(now)) {
                refund(reservation.id, now);
            }
        }
    }

    private Usage usageFor(UserKey userKey, LocalDate quotaDate, QuotaPolicy policy) {
        return usageByUserAndDate.computeIfAbsent(
                usageKey(userKey.value(), quotaDate),
                ignored -> new Usage(policy.dailyLimit(), 0, 0, 0, policy.unlimited()));
    }

    private static DailyQuotaStatus status(LocalDate quotaDate, Usage usage, OffsetDateTime resetAt) {
        int remaining = usage.unlimited ? Integer.MAX_VALUE : Math.max(0, usage.dailyLimit + usage.bonus - usage.used - usage.reserved);
        return new DailyQuotaStatus(quotaDate, usage.dailyLimit, usage.used + usage.reserved, usage.bonus, remaining, usage.unlimited, resetAt);
    }

    private static String usageKey(String userKey, LocalDate quotaDate) {
        return userKey + ":" + quotaDate;
    }

    private static String idempotencyScope(UserKey userKey, QuotaRequestType requestType, String idempotencyKey) {
        return userKey.value() + ":" + requestType.name() + ":" + idempotencyKey;
    }

    private static final class Usage {
        private final int dailyLimit;
        private int used;
        private int reserved;
        private final int bonus;
        private final boolean unlimited;

        private Usage(int dailyLimit, int used, int reserved, int bonus, boolean unlimited) {
            this.dailyLimit = dailyLimit;
            this.used = used;
            this.reserved = reserved;
            this.bonus = bonus;
            this.unlimited = unlimited;
        }
    }

    private static final class StoredReservation {
        private final String id;
        private final String userKey;
        private final LocalDate quotaDate;
        private final QuotaRequestType requestType;
        private final String idempotencyKey;
        private QuotaReservationStatus status;
        private final Instant expiresAt;

        private StoredReservation(
                String id,
                String userKey,
                LocalDate quotaDate,
                QuotaRequestType requestType,
                String idempotencyKey,
                QuotaReservationStatus status,
                Instant expiresAt
        ) {
            this.id = id;
            this.userKey = userKey;
            this.quotaDate = quotaDate;
            this.requestType = requestType;
            this.idempotencyKey = idempotencyKey;
            this.status = status;
            this.expiresAt = expiresAt;
        }

        private QuotaReservation toReservation() {
            return new QuotaReservation(id, userKey, quotaDate, requestType, idempotencyKey, status);
        }
    }
}
