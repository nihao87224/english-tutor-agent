package cn.forever24.tutor.application.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface AdminRepository {

    AdminDashboardSummary dashboard(LocalDate quotaDate, Instant dayStart);

    AdminPage<AdminUserSummary> searchUsers(String query, String status, String role, int page, int size);

    AdminUserDetail requireUser(String userKey);

    AdminUserDetail updateUserStatus(String userKey, String status, long actorUserId, Instant now);

    AdminUserDetail replaceUserRoles(String userKey, Set<String> roles, long actorUserId, Instant now);

    AdminQuotaState updateQuotaPolicy(
            String userKey,
            Integer dailyLimitOverride,
            boolean unlimited,
            long actorUserId,
            LocalDate quotaDate,
            int defaultDailyLimit,
            Instant now);

    AdminQuotaState resetTodayQuota(String userKey, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now);

    AdminQuotaState addQuotaBonus(String userKey, int bonus, long actorUserId, LocalDate quotaDate, int defaultDailyLimit, Instant now);

    List<AdminSystemSetting> listSettings();

    AdminSystemSetting updateSetting(String key, String value, String valueType, String description, long actorUserId, Instant now);

    AdminPage<AdminAuditEntry> listAudit(int page, int size);
}
