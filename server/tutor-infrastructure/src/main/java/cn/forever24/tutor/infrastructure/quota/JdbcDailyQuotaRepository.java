package cn.forever24.tutor.infrastructure.quota;

import cn.forever24.tutor.application.quota.DailyQuotaRepository;
import cn.forever24.tutor.application.quota.DailyQuotaStatus;
import cn.forever24.tutor.application.quota.QuotaException;
import cn.forever24.tutor.application.quota.QuotaPolicy;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import cn.forever24.tutor.application.quota.QuotaReservationStatus;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class JdbcDailyQuotaRepository implements DailyQuotaRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDailyQuotaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DailyQuotaStatus getStatus(
            UserKey userKey,
            LocalDate quotaDate,
            QuotaPolicy policy,
            OffsetDateTime resetAt
    ) {
        long userId = requireUserId(userKey);
        ensureUsage(userId, quotaDate, policy, Instant.now());
        UsageRow row = usageRow(userId, quotaDate);
        return row.toStatus(quotaDate, resetAt);
    }

    @Override
    @Transactional
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
        ExistingReservation existing = findByIdempotency(userKey, requestType, idempotencyKey);
        if (existing != null) {
            return existing.toReservation();
        }

        long userId = requireUserId(userKey);
        ensureUsage(userId, quotaDate, policy, now);
        String reservationId = "quota-" + UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                            INSERT INTO quota_reservation
                                (id, user_id, quota_date, request_type, idempotency_key, status,
                                 expires_at_utc, created_at_utc, updated_at_utc, version)
                            VALUES (?, ?, ?, ?, ?, 'RESERVED', ?, ?, ?, 0)
                            """,
                    reservationId,
                    userId,
                    Date.valueOf(quotaDate),
                    requestType.name(),
                    idempotencyKey,
                    Timestamp.from(expiresAt),
                    Timestamp.from(now),
                    Timestamp.from(now));
        } catch (DuplicateKeyException exception) {
            return findByIdempotency(userKey, requestType, idempotencyKey).toReservation();
        }

        int updated = jdbcTemplate.update("""
                        UPDATE quota_daily_usage
                        SET reserved_count = reserved_count + 1,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ?
                          AND quota_date = ?
                          AND (unlimited = TRUE OR used_count + reserved_count < daily_limit + bonus_count)
                        """,
                Timestamp.from(now),
                userId,
                Date.valueOf(quotaDate));
        if (updated == 0) {
            jdbcTemplate.update("""
                            UPDATE quota_reservation
                            SET status = 'REFUNDED',
                                refunded_at_utc = ?,
                                updated_at_utc = ?,
                                version = version + 1
                            WHERE id = ?
                            """,
                    Timestamp.from(now),
                    Timestamp.from(now),
                    reservationId);
            throw QuotaException.exceeded(usageRow(userId, quotaDate).toStatus(quotaDate, resetAt));
        }
        return new QuotaReservation(
                reservationId,
                userKey.value(),
                quotaDate,
                requestType,
                idempotencyKey,
                QuotaReservationStatus.RESERVED);
    }

    @Override
    @Transactional
    public void commit(String reservationId, Instant now) {
        ReservationRow row = findById(reservationId);
        if (row == null || row.status() != QuotaReservationStatus.RESERVED) {
            return;
        }
        int updated = jdbcTemplate.update("""
                        UPDATE quota_reservation
                        SET status = 'COMMITTED',
                            committed_at_utc = ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE id = ? AND status = 'RESERVED'
                        """,
                Timestamp.from(now),
                Timestamp.from(now),
                reservationId);
        if (updated == 1) {
            jdbcTemplate.update("""
                            UPDATE quota_daily_usage
                            SET reserved_count = GREATEST(0, reserved_count - 1),
                                used_count = used_count + 1,
                                updated_at_utc = ?,
                                version = version + 1
                            WHERE user_id = ? AND quota_date = ?
                            """,
                    Timestamp.from(now),
                    row.userId(),
                    Date.valueOf(row.quotaDate()));
        }
    }

    @Override
    @Transactional
    public void refund(String reservationId, Instant now) {
        ReservationRow row = findById(reservationId);
        if (row == null || row.status() != QuotaReservationStatus.RESERVED) {
            return;
        }
        int updated = jdbcTemplate.update("""
                        UPDATE quota_reservation
                        SET status = 'REFUNDED',
                            refunded_at_utc = ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE id = ? AND status = 'RESERVED'
                        """,
                Timestamp.from(now),
                Timestamp.from(now),
                reservationId);
        if (updated == 1) {
            jdbcTemplate.update("""
                            UPDATE quota_daily_usage
                            SET reserved_count = GREATEST(0, reserved_count - 1),
                                updated_at_utc = ?,
                                version = version + 1
                            WHERE user_id = ? AND quota_date = ?
                            """,
                    Timestamp.from(now),
                    row.userId(),
                    Date.valueOf(row.quotaDate()));
        }
    }

    @Override
    public void refundStaleReservations(Instant now) {
        List<String> staleIds = jdbcTemplate.queryForList("""
                        SELECT id
                        FROM quota_reservation
                        WHERE status = 'RESERVED' AND expires_at_utc <= ?
                        LIMIT 100
                        """,
                String.class,
                Timestamp.from(now));
        for (String id : staleIds) {
            refund(id, now);
        }
    }

    private void ensureUsage(long userId, LocalDate quotaDate, QuotaPolicy defaultPolicy, Instant now) {
        EffectivePolicy policy = effectivePolicy(userId, defaultPolicy);
        jdbcTemplate.update("""
                        INSERT IGNORE INTO quota_daily_usage
                            (user_id, quota_date, daily_limit, bonus_count, used_count, reserved_count,
                             unlimited, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, 0, 0, 0, ?, ?, ?, 0)
                        """,
                userId,
                Date.valueOf(quotaDate),
                policy.dailyLimit(),
                policy.unlimited(),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private EffectivePolicy effectivePolicy(long userId, QuotaPolicy defaultPolicy) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                COALESCE(daily_limit_override, ?) AS daily_limit,
                                unlimited
                            FROM quota_user_policy
                            WHERE user_id = ?
                            """,
                    (rs, rowNum) -> new EffectivePolicy(rs.getInt("daily_limit"), rs.getBoolean("unlimited")),
                    defaultPolicy.dailyLimit(),
                    userId);
        } catch (EmptyResultDataAccessException exception) {
            return new EffectivePolicy(defaultPolicy.dailyLimit(), defaultPolicy.unlimited());
        }
    }

    private long requireUserId(UserKey userKey) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE'",
                    Long.class,
                    userKey.value());
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("active user was not found");
        }
    }

    private UsageRow usageRow(long userId, LocalDate quotaDate) {
        return jdbcTemplate.queryForObject("""
                        SELECT daily_limit, bonus_count, used_count, reserved_count, unlimited
                        FROM quota_daily_usage
                        WHERE user_id = ? AND quota_date = ?
                        """,
                (rs, rowNum) -> new UsageRow(
                        rs.getInt("daily_limit"),
                        rs.getInt("bonus_count"),
                        rs.getInt("used_count"),
                        rs.getInt("reserved_count"),
                        rs.getBoolean("unlimited")),
                userId,
                Date.valueOf(quotaDate));
    }

    private ExistingReservation findByIdempotency(UserKey userKey, QuotaRequestType requestType, String idempotencyKey) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT r.id, r.quota_date, r.status
                            FROM quota_reservation r
                            JOIN app_user u ON u.id = r.user_id
                            WHERE u.user_key = ?
                              AND r.request_type = ?
                              AND r.idempotency_key = ?
                            """,
                    (rs, rowNum) -> new ExistingReservation(
                            rs.getString("id"),
                            userKey.value(),
                            rs.getDate("quota_date").toLocalDate(),
                            requestType,
                            idempotencyKey,
                            QuotaReservationStatus.valueOf(rs.getString("status"))),
                    userKey.value(),
                    requestType.name(),
                    idempotencyKey);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private ReservationRow findById(String reservationId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT user_id, quota_date, status
                            FROM quota_reservation
                            WHERE id = ?
                            """,
                    (rs, rowNum) -> new ReservationRow(
                            rs.getLong("user_id"),
                            rs.getDate("quota_date").toLocalDate(),
                            QuotaReservationStatus.valueOf(rs.getString("status"))),
                    reservationId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private record EffectivePolicy(int dailyLimit, boolean unlimited) {
    }

    private record UsageRow(int dailyLimit, int bonus, int used, int reserved, boolean unlimited) {
        private DailyQuotaStatus toStatus(LocalDate quotaDate, OffsetDateTime resetAt) {
            int effectiveUsed = used + reserved;
            int remaining = unlimited ? Integer.MAX_VALUE : Math.max(0, dailyLimit + bonus - effectiveUsed);
            return new DailyQuotaStatus(quotaDate, dailyLimit, effectiveUsed, bonus, remaining, unlimited, resetAt);
        }
    }

    private record ExistingReservation(
            String id,
            String userKey,
            LocalDate quotaDate,
            QuotaRequestType requestType,
            String idempotencyKey,
            QuotaReservationStatus status
    ) {
        private QuotaReservation toReservation() {
            return new QuotaReservation(id, userKey, quotaDate, requestType, idempotencyKey, status);
        }
    }

    private record ReservationRow(long userId, LocalDate quotaDate, QuotaReservationStatus status) {
    }
}
