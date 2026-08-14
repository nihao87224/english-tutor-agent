package cn.forever24.tutor.application.admin;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminRepository repository;
    private final Clock clock;
    private final ZoneId quotaResetZone;
    private final int defaultDailyLimit;

    public AdminApplicationService(AdminRepository repository, Clock clock, ZoneId quotaResetZone, int defaultDailyLimit) {
        this.repository = repository;
        this.clock = clock;
        this.quotaResetZone = quotaResetZone;
        this.defaultDailyLimit = defaultDailyLimit;
    }

    public AdminDashboardSummary dashboard() {
        Instant now = clock.instant();
        LocalDate quotaDate = LocalDate.ofInstant(now, quotaResetZone);
        Instant dayStart = quotaDate.atStartOfDay(quotaResetZone).toInstant();
        return repository.dashboard(quotaDate, dayStart);
    }

    public AdminPage<AdminUserSummary> searchUsers(String query, String status, String role, Integer page, Integer size) {
        return repository.searchUsers(blankToNull(query), blankToNull(status), blankToNull(role), page(page), size(size));
    }

    public AdminUserDetail getUser(String userKey) {
        return repository.requireUser(requireNonBlank(userKey, "userKey"));
    }

    public AdminUserDetail updateUserStatus(String userKey, String status, long actorUserId) {
        String normalizedStatus = requireNonBlank(status, "status").toUpperCase();
        if (!Set.of("ACTIVE", "DISABLED").contains(normalizedStatus)) {
            throw AdminException.invalid("status must be ACTIVE or DISABLED");
        }
        return repository.updateUserStatus(requireNonBlank(userKey, "userKey"), normalizedStatus, requireActor(actorUserId), clock.instant());
    }

    public AdminUserDetail replaceUserRoles(String userKey, Set<String> roles, long actorUserId) {
        if (roles == null || roles.isEmpty()) {
            throw AdminException.invalid("roles are required");
        }
        Set<String> normalized = roles.stream()
                .map(role -> requireNonBlank(role, "role").toUpperCase())
                .collect(Collectors.toUnmodifiableSet());
        return repository.replaceUserRoles(requireNonBlank(userKey, "userKey"), normalized, requireActor(actorUserId), clock.instant());
    }

    public AdminQuotaState updateQuotaPolicy(String userKey, Integer dailyLimitOverride, boolean unlimited, long actorUserId) {
        if (dailyLimitOverride != null && dailyLimitOverride < 0) {
            throw AdminException.invalid("dailyLimitOverride must be >= 0");
        }
        return repository.updateQuotaPolicy(
                requireNonBlank(userKey, "userKey"),
                dailyLimitOverride,
                unlimited,
                requireActor(actorUserId),
                currentQuotaDate(),
                defaultDailyLimit,
                clock.instant());
    }

    public AdminQuotaState resetTodayQuota(String userKey, long actorUserId) {
        return repository.resetTodayQuota(
                requireNonBlank(userKey, "userKey"),
                requireActor(actorUserId),
                currentQuotaDate(),
                defaultDailyLimit,
                clock.instant());
    }

    public AdminQuotaState addQuotaBonus(String userKey, int bonus, long actorUserId) {
        if (bonus <= 0) {
            throw AdminException.invalid("bonus must be positive");
        }
        return repository.addQuotaBonus(
                requireNonBlank(userKey, "userKey"),
                bonus,
                requireActor(actorUserId),
                currentQuotaDate(),
                defaultDailyLimit,
                clock.instant());
    }

    public List<AdminSystemSetting> listSettings() {
        return repository.listSettings();
    }

    public AdminSystemSetting updateSetting(
            String key,
            String value,
            String valueType,
            String description,
            long actorUserId
    ) {
        String normalizedValueType = requireNonBlank(valueType, "valueType").toUpperCase();
        if (!Set.of("STRING", "INTEGER", "BOOLEAN", "JSON").contains(normalizedValueType)) {
            throw AdminException.invalid("valueType must be STRING, INTEGER, BOOLEAN or JSON");
        }
        return repository.updateSetting(
                requireNonBlank(key, "key"),
                requireNonBlank(value, "value"),
                normalizedValueType,
                description == null ? "" : description.trim(),
                requireActor(actorUserId),
                clock.instant());
    }

    public AdminPage<AdminAuditEntry> listAudit(Integer page, Integer size) {
        return repository.listAudit(page(page), size(size));
    }

    private LocalDate currentQuotaDate() {
        return LocalDate.ofInstant(clock.instant(), quotaResetZone);
    }

    private static int page(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw AdminException.invalid("page must be >= 0");
        }
        return page;
    }

    private static int size(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw AdminException.invalid("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private static long requireActor(long actorUserId) {
        if (actorUserId <= 0) {
            throw AdminException.invalid("actor user id is required");
        }
        return actorUserId;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw AdminException.invalid(fieldName + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
