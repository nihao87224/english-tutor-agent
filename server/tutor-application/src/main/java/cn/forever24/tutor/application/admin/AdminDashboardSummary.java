package cn.forever24.tutor.application.admin;

public record AdminDashboardSummary(
        long totalUsers,
        long activeUsersToday,
        long newUsersToday,
        long aiRequestsToday,
        long usersReachedQuotaLimit,
        String activeDefaultProvider
) {
}
