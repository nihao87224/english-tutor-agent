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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryAdminRepository implements AdminRepository {

    private final Map<String, AdminSystemSetting> settings = new LinkedHashMap<>();
    private final List<AdminAuditEntry> auditEntries = new ArrayList<>();
    private long nextAuditId = 1;

    public InMemoryAdminRepository() {
        Instant now = Instant.EPOCH;
        settings.put("platform.defaultLocale", new AdminSystemSetting("platform.defaultLocale", "zh-CN", "STRING", "Default UI locale", now));
        settings.put("quota.defaultDailyLimit", new AdminSystemSetting("quota.defaultDailyLimit", "50", "INTEGER", "Default daily AI request limit", now));
        settings.put("maintenance.enabled", new AdminSystemSetting("maintenance.enabled", "false", "BOOLEAN", "Maintenance mode", now));
    }

    @Override
    public synchronized AdminDashboardSummary dashboard(LocalDate quotaDate, Instant dayStart) {
        return new AdminDashboardSummary(0, 0, 0, 0, 0, "openai");
    }

    @Override
    public synchronized AdminPage<AdminUserSummary> searchUsers(String query, String status, String role, int page, int size) {
        return new AdminPage<>(List.of(), page, size, 0);
    }

    @Override
    public AdminUserDetail requireUser(String userKey) {
        throw AdminException.notFound("user was not found: " + userKey);
    }

    @Override
    public AdminUserDetail updateUserStatus(String userKey, String status, long actorUserId, Instant now) {
        throw AdminException.notFound("user was not found: " + userKey);
    }

    @Override
    public AdminUserDetail replaceUserRoles(String userKey, Set<String> roles, long actorUserId, Instant now) {
        throw AdminException.notFound("user was not found: " + userKey);
    }

    @Override
    public AdminQuotaState updateQuotaPolicy(String userKey, Integer dailyLimitOverride, boolean unlimited, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now) {
        audit(actorUserId, "USER_QUOTA_POLICY_UPDATED", "USER", userKey, now);
        return new AdminQuotaState(userKey, dailyLimitOverride, unlimited, quotaDate, dailyLimitOverride == null ? defaultDailyLimit : dailyLimitOverride, 0, 0, 0, unlimited ? Integer.MAX_VALUE : dailyLimitOverride == null ? defaultDailyLimit : dailyLimitOverride);
    }

    @Override
    public AdminQuotaState resetTodayQuota(String userKey, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now) {
        audit(actorUserId, "USER_QUOTA_RESET", "USER", userKey, now);
        return new AdminQuotaState(userKey, null, false, quotaDate, defaultDailyLimit, 0, 0, 0, defaultDailyLimit);
    }

    @Override
    public AdminQuotaState addQuotaBonus(String userKey, int bonus, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now) {
        audit(actorUserId, "USER_QUOTA_BONUS_ADDED", "USER", userKey, now);
        return new AdminQuotaState(userKey, null, false, quotaDate, defaultDailyLimit, 0, 0, bonus, defaultDailyLimit + bonus);
    }

    @Override
    public synchronized List<AdminSystemSetting> listSettings() {
        return List.copyOf(settings.values());
    }

    @Override
    public synchronized AdminSystemSetting updateSetting(String key, String value, String valueType, String description, long actorUserId, Instant now) {
        AdminSystemSetting setting = new AdminSystemSetting(key, value, valueType, description, now);
        settings.put(key, setting);
        audit(actorUserId, "SYSTEM_SETTING_UPDATED", "SYSTEM_SETTING", key, now);
        return setting;
    }

    @Override
    public synchronized AdminPage<AdminAuditEntry> listAudit(int page, int size) {
        int from = Math.min(page * size, auditEntries.size());
        int to = Math.min(from + size, auditEntries.size());
        return new AdminPage<>(auditEntries.subList(from, to), page, size, auditEntries.size());
    }

    private synchronized void audit(long actorUserId, String actionCode, String targetType, String targetKey, Instant now) {
        auditEntries.add(0, new AdminAuditEntry(nextAuditId++, actorUserId, null, actionCode, targetType, targetKey, now));
    }
}
