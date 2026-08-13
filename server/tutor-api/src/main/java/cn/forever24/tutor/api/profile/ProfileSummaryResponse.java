package cn.forever24.tutor.api.profile;

import cn.forever24.tutor.profile.ProfileSummary;

public record ProfileSummaryResponse(
        String primaryGoal,
        int dailyMinutes,
        String correctionStyle,
        boolean onboardingCompleted
) {

    public static ProfileSummaryResponse from(ProfileSummary summary) {
        return new ProfileSummaryResponse(
                summary.primaryGoal().name(),
                summary.dailyMinutes(),
                summary.correctionStyle().name(),
                summary.onboardingCompleted());
    }
}
