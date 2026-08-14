package cn.forever24.tutor.infrastructure.admin;

import cn.forever24.tutor.application.admin.AdminAuditEntry;
import cn.forever24.tutor.application.admin.AdminDashboardSummary;
import cn.forever24.tutor.application.admin.AdminException;
import cn.forever24.tutor.application.admin.AdminPage;
import cn.forever24.tutor.application.admin.AdminQuotaState;
import cn.forever24.tutor.application.admin.AdminRepository;
import cn.forever24.tutor.application.admin.AdminSystemSetting;
import cn.forever24.tutor.application.admin.AdminUserDetail;
import cn.forever24.tutor.application.admin.AdminUserSummary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcAdminRepository implements AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminDashboardSummary dashboard(LocalDate quotaDate, Instant dayStart) {
        long totalUsers = count("SELECT COUNT(*) FROM app_user WHERE deleted_at_utc IS NULL");
        long activeUsersToday = count("SELECT COUNT(*) FROM app_user WHERE last_login_at_utc >= ?", Timestamp.from(dayStart));
        long newUsersToday = count("SELECT COUNT(*) FROM app_user WHERE created_at_utc >= ?", Timestamp.from(dayStart));
        long aiRequestsToday = count("SELECT COALESCE(SUM(used_count), 0) FROM quota_daily_usage WHERE quota_date = ?", Date.valueOf(quotaDate));
        long reachedQuota = count("""
                SELECT COUNT(*)
                FROM quota_daily_usage
                WHERE quota_date = ?
                  AND unlimited = FALSE
                  AND used_count + reserved_count >= daily_limit + bonus_count
                """, Date.valueOf(quotaDate));
        String provider = firstString("""
                SELECT provider_code
                FROM ai_provider_config
                WHERE enabled = TRUE AND default_llm = TRUE
                ORDER BY provider_code
                LIMIT 1
                """, "openai");
        return new AdminDashboardSummary(totalUsers, activeUsersToday, newUsersToday, aiRequestsToday, reachedQuota, provider);
    }

    @Override
    public AdminPage<AdminUserSummary> searchUsers(String query, String status, String role, int page, int size) {
        StringBuilder where = new StringBuilder("WHERE u.deleted_at_utc IS NULL");
        new Filters(query, status, role).append(where);
        Object[] args = new Filters(query, status, role).args();
        long total = count("SELECT COUNT(DISTINCT u.id) FROM app_user u " + where, args);
        List<AdminUserSummary> users = jdbcTemplate.query("""
                        SELECT u.id, u.user_key, u.email, u.status, u.created_at_utc, u.last_login_at_utc,
                               GROUP_CONCAT(r.code ORDER BY r.code SEPARATOR ',') AS roles
                        FROM app_user u
                        LEFT JOIN app_user_role ur ON ur.user_id = u.id
                        LEFT JOIN app_role r ON r.id = ur.role_id
                        %s
                        GROUP BY u.id, u.user_key, u.email, u.status, u.created_at_utc, u.last_login_at_utc
                        ORDER BY u.created_at_utc DESC, u.id DESC
                        LIMIT ? OFFSET ?
                        """.formatted(where),
                (rs, rowNum) -> new AdminUserSummary(
                        rs.getLong("id"),
                        rs.getString("user_key"),
                        rs.getString("email"),
                        rs.getString("status"),
                        splitCodes(rs.getString("roles")),
                        toInstant(rs.getTimestamp("created_at_utc")),
                        toInstant(rs.getTimestamp("last_login_at_utc"))),
                append(args, size, page * size));
        return new AdminPage<>(users, page, size, total);
    }

    @Override
    public AdminUserDetail requireUser(String userKey) {
        Long userId = userId(userKey);
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, user_key, email, status, locale, timezone, auth_version,
                                   created_at_utc, updated_at_utc, last_login_at_utc, disabled_at_utc
                            FROM app_user
                            WHERE id = ?
                            """,
                    (rs, rowNum) -> new AdminUserDetail(
                            rs.getLong("id"),
                            rs.getString("user_key"),
                            rs.getString("email"),
                            rs.getString("status"),
                            rs.getString("locale"),
                            rs.getString("timezone"),
                            rs.getLong("auth_version"),
                            rolesForUser(userId),
                            authoritiesForUser(userId),
                            toInstant(rs.getTimestamp("created_at_utc")),
                            toInstant(rs.getTimestamp("updated_at_utc")),
                            toInstant(rs.getTimestamp("last_login_at_utc")),
                            toInstant(rs.getTimestamp("disabled_at_utc"))),
                    userId);
        } catch (EmptyResultDataAccessException exception) {
            throw AdminException.notFound("user was not found: " + userKey);
        }
    }

    @Override
    @Transactional
    public AdminUserDetail updateUserStatus(String userKey, String status, long actorUserId, Instant now) {
        long userId = userId(userKey);
        jdbcTemplate.update("""
                        UPDATE app_user
                        SET status = ?,
                            disabled_at_utc = ?,
                            updated_at_utc = ?,
                            auth_version = auth_version + 1,
                            version = version + 1
                        WHERE id = ?
                        """,
                status,
                "DISABLED".equals(status) ? Timestamp.from(now) : null,
                Timestamp.from(now),
                userId);
        writeAudit(actorUserId, "USER_STATUS_UPDATED", "USER", userKey, now);
        return requireUser(userKey);
    }

    @Override
    @Transactional
    public AdminUserDetail replaceUserRoles(String userKey, Set<String> roles, long actorUserId, Instant now) {
        long userId = userId(userKey);
        validateRoles(roles);
        jdbcTemplate.update("DELETE FROM app_user_role WHERE user_id = ?", userId);
        for (String role : roles) {
            jdbcTemplate.update("""
                            INSERT INTO app_user_role (user_id, role_id, created_at_utc)
                            SELECT ?, id, ?
                            FROM app_role
                            WHERE code = ?
                            """,
                    userId,
                    Timestamp.from(now),
                    role);
        }
        jdbcTemplate.update("""
                        UPDATE app_user
                        SET updated_at_utc = ?,
                            auth_version = auth_version + 1,
                            version = version + 1
                        WHERE id = ?
                        """,
                Timestamp.from(now),
                userId);
        writeAudit(actorUserId, "USER_ROLES_REPLACED", "USER", userKey, now);
        return requireUser(userKey);
    }

    @Override
    @Transactional
    public AdminQuotaState updateQuotaPolicy(String userKey, Integer dailyLimitOverride, boolean unlimited, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now) {
        long userId = userId(userKey);
        jdbcTemplate.update("""
                        INSERT INTO quota_user_policy
                            (user_id, daily_limit_override, unlimited, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            daily_limit_override = VALUES(daily_limit_override),
                            unlimited = VALUES(unlimited),
                            updated_at_utc = VALUES(updated_at_utc),
                            version = version + 1
                        """,
                userId,
                dailyLimitOverride,
                unlimited,
                Timestamp.from(now),
                Timestamp.from(now));
        writeAudit(actorUserId, "USER_QUOTA_POLICY_UPDATED", "USER", userKey, now);
        ensureUsage(userId, quotaDate, effectiveLimit(userId, defaultDailyLimit), effectiveUnlimited(userId), now);
        return quotaState(userKey, userId, quotaDate, defaultDailyLimit);
    }

    @Override
    @Transactional
    public AdminQuotaState resetTodayQuota(String userKey, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now) {
        long userId = userId(userKey);
        ensureUsage(userId, quotaDate, effectiveLimit(userId, defaultDailyLimit), effectiveUnlimited(userId), now);
        jdbcTemplate.update("""
                        UPDATE quota_daily_usage
                        SET used_count = 0,
                            reserved_count = 0,
                            bonus_count = 0,
                            daily_limit = ?,
                            unlimited = ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ? AND quota_date = ?
                        """,
                effectiveLimit(userId, defaultDailyLimit),
                effectiveUnlimited(userId),
                Timestamp.from(now),
                userId,
                Date.valueOf(quotaDate));
        writeAudit(actorUserId, "USER_QUOTA_RESET", "USER", userKey, now);
        return quotaState(userKey, userId, quotaDate, defaultDailyLimit);
    }

    @Override
    @Transactional
    public AdminQuotaState addQuotaBonus(String userKey, int bonus, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now) {
        long userId = userId(userKey);
        ensureUsage(userId, quotaDate, effectiveLimit(userId, defaultDailyLimit), effectiveUnlimited(userId), now);
        jdbcTemplate.update("""
                        UPDATE quota_daily_usage
                        SET bonus_count = bonus_count + ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ? AND quota_date = ?
                        """,
                bonus,
                Timestamp.from(now),
                userId,
                Date.valueOf(quotaDate));
        writeAudit(actorUserId, "USER_QUOTA_BONUS_ADDED", "USER", userKey, now);
        return quotaState(userKey, userId, quotaDate, defaultDailyLimit);
    }

    @Override
    public List<AdminSystemSetting> listSettings() {
        return jdbcTemplate.query("""
                        SELECT setting_key, setting_value, value_type, description, updated_at_utc
                        FROM system_setting
                        ORDER BY setting_key
                        """,
                (rs, rowNum) -> new AdminSystemSetting(
                        rs.getString("setting_key"),
                        rs.getString("setting_value"),
                        rs.getString("value_type"),
                        rs.getString("description"),
                        toInstant(rs.getTimestamp("updated_at_utc"))));
    }

    @Override
    @Transactional
    public AdminSystemSetting updateSetting(String key, String value, String valueType, String description, long actorUserId, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO system_setting
                            (setting_key, setting_value, value_type, description, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            setting_value = VALUES(setting_value),
                            value_type = VALUES(value_type),
                            description = VALUES(description),
                            updated_at_utc = VALUES(updated_at_utc),
                            version = version + 1
                        """,
                key,
                value,
                valueType,
                description,
                Timestamp.from(now),
                Timestamp.from(now));
        writeAudit(actorUserId, "SYSTEM_SETTING_UPDATED", "SYSTEM_SETTING", key, now);
        return listSettings().stream()
                .filter(setting -> setting.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public AdminPage<AdminAuditEntry> listAudit(int page, int size) {
        long total = count("SELECT COUNT(*) FROM admin_audit_log");
        List<AdminAuditEntry> entries = jdbcTemplate.query("""
                        SELECT a.id, a.actor_user_id, u.email AS actor_email, a.action_code,
                               a.target_type, a.target_key, a.created_at_utc
                        FROM admin_audit_log a
                        LEFT JOIN app_user u ON u.id = a.actor_user_id
                        ORDER BY a.created_at_utc DESC, a.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new AdminAuditEntry(
                        rs.getLong("id"),
                        nullableLong(rs.getObject("actor_user_id")),
                        rs.getString("actor_email"),
                        rs.getString("action_code"),
                        rs.getString("target_type"),
                        rs.getString("target_key"),
                        toInstant(rs.getTimestamp("created_at_utc"))),
                size,
                page * size);
        return new AdminPage<>(entries, page, size, total);
    }

    private void ensureUsage(long userId, LocalDate quotaDate, int dailyLimit, boolean unlimited, Instant now) {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO quota_daily_usage
                            (user_id, quota_date, daily_limit, bonus_count, used_count, reserved_count,
                             unlimited, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, 0, 0, 0, ?, ?, ?, 0)
                        """,
                userId,
                Date.valueOf(quotaDate),
                dailyLimit,
                unlimited,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private AdminQuotaState quotaState(String userKey, long userId, LocalDate quotaDate, int defaultDailyLimit) {
        QuotaPolicyRow policy = quotaPolicy(userId);
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT daily_limit, used_count, reserved_count, bonus_count, unlimited
                            FROM quota_daily_usage
                            WHERE user_id = ? AND quota_date = ?
                            """,
                    (rs, rowNum) -> {
                        boolean unlimited = rs.getBoolean("unlimited");
                        int dailyLimit = rs.getInt("daily_limit");
                        int used = rs.getInt("used_count");
                        int reserved = rs.getInt("reserved_count");
                        int bonus = rs.getInt("bonus_count");
                        int remaining = unlimited ? Integer.MAX_VALUE : Math.max(0, dailyLimit + bonus - used - reserved);
                        return new AdminQuotaState(userKey, policy.dailyLimitOverride(), policy.unlimited(), quotaDate, dailyLimit, used, reserved, bonus, remaining);
                    },
                    userId,
                    Date.valueOf(quotaDate));
        } catch (EmptyResultDataAccessException exception) {
            int limit = effectiveLimit(userId, defaultDailyLimit);
            boolean unlimited = effectiveUnlimited(userId);
            return new AdminQuotaState(userKey, policy.dailyLimitOverride(), policy.unlimited(), quotaDate, limit, 0, 0, 0, unlimited ? Integer.MAX_VALUE : limit);
        }
    }

    private int effectiveLimit(long userId, int defaultDailyLimit) {
        QuotaPolicyRow policy = quotaPolicy(userId);
        return policy.dailyLimitOverride() == null ? defaultDailyLimit : policy.dailyLimitOverride();
    }

    private boolean effectiveUnlimited(long userId) {
        return quotaPolicy(userId).unlimited();
    }

    private QuotaPolicyRow quotaPolicy(long userId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT daily_limit_override, unlimited
                            FROM quota_user_policy
                            WHERE user_id = ?
                            """,
                    (rs, rowNum) -> new QuotaPolicyRow((Integer) rs.getObject("daily_limit_override"), rs.getBoolean("unlimited")),
                    userId);
        } catch (EmptyResultDataAccessException exception) {
            return new QuotaPolicyRow(null, false);
        }
    }

    private Long userId(String userKey) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE user_key = ? AND deleted_at_utc IS NULL", Long.class, userKey);
        } catch (EmptyResultDataAccessException exception) {
            throw AdminException.notFound("user was not found: " + userKey);
        }
    }

    private void validateRoles(Set<String> roles) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_role WHERE code IN (" + placeholders(roles.size()) + ")",
                Integer.class,
                roles.toArray());
        if (count == null || count != roles.size()) {
            throw AdminException.invalid("unknown role code");
        }
    }

    private Set<String> rolesForUser(long userId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                        SELECT r.code
                        FROM app_user_role ur
                        JOIN app_role r ON r.id = ur.role_id
                        WHERE ur.user_id = ?
                        ORDER BY r.code
                        """,
                String.class,
                userId));
    }

    private Set<String> authoritiesForUser(long userId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                        SELECT p.code
                        FROM app_user_role ur
                        JOIN app_role_permission rp ON rp.role_id = ur.role_id
                        JOIN app_permission p ON p.id = rp.permission_id
                        WHERE ur.user_id = ?
                        ORDER BY p.code
                        """,
                String.class,
                userId));
    }

    private void writeAudit(long actorUserId, String actionCode, String targetType, String targetKey, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO admin_audit_log
                            (actor_user_id, action_code, target_type, target_key, created_at_utc)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                actorUserId,
                actionCode,
                targetType,
                targetKey,
                Timestamp.from(now));
    }

    private long count(String sql, Object... args) {
        Number number = jdbcTemplate.queryForObject(sql, Number.class, args);
        return number == null ? 0 : number.longValue();
    }

    private String firstString(String sql, String fallback) {
        try {
            String value = jdbcTemplate.queryForObject(sql, String.class);
            return value == null || value.isBlank() ? fallback : value;
        } catch (EmptyResultDataAccessException exception) {
            return fallback;
        }
    }

    private static Set<String> splitCodes(String joinedCodes) {
        if (joinedCodes == null || joinedCodes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(joinedCodes.split(",")).collect(Collectors.toUnmodifiableSet());
    }

    private static Object[] append(Object[] args, Object... tail) {
        Object[] merged = Arrays.copyOf(args, args.length + tail.length);
        System.arraycopy(tail, 0, merged, args.length, tail.length);
        return merged;
    }

    private static String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private record QuotaPolicyRow(Integer dailyLimitOverride, boolean unlimited) {
    }

    private record Filters(String query, String status, String role) {
        private void append(StringBuilder where) {
            if (query != null) {
                where.append(" AND (LOWER(u.email) LIKE ? OR LOWER(u.user_key) LIKE ?)");
            }
            if (status != null) {
                where.append(" AND u.status = ?");
            }
            if (role != null) {
                where.append("""
                         AND EXISTS (
                            SELECT 1
                            FROM app_user_role fur
                            JOIN app_role fr ON fr.id = fur.role_id
                            WHERE fur.user_id = u.id AND fr.code = ?
                         )
                        """);
            }
        }

        private Object[] args() {
            List<Object> args = new java.util.ArrayList<>();
            if (query != null) {
                String value = "%" + query.toLowerCase() + "%";
                args.add(value);
                args.add(value);
            }
            if (status != null) {
                args.add(status.toUpperCase());
            }
            if (role != null) {
                args.add(role.toUpperCase());
            }
            return args.toArray();
        }
    }
}
