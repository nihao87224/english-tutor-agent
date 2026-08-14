package cn.forever24.tutor.api.admin;

public record AdminQuotaPolicyRequest(Integer dailyLimitOverride, boolean unlimited) {
}
