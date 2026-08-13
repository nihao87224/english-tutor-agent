package cn.forever24.tutor.profile;

public record ProfileSummary(
        PrimaryGoal primaryGoal,
        int dailyMinutes,
        CorrectionStyle correctionStyle,
        boolean reminderEnabled,
        RawContentRetention rawTextRetention,
        RawContentRetention rawAudioRetention,
        int rawAudioRetentionDays,
        boolean onboardingCompleted,
        long profileVersion
) {
}
