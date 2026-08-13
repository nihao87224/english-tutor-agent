package cn.forever24.tutor.infrastructure.profile;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.profile.CorrectionStyle;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserProfileRepository implements UserProfileRepository {

    private final Map<UserKey, ProfileSummary> profiles = new ConcurrentHashMap<>();
    private final Map<UserKey, OnboardingStep> onboardingSteps = new ConcurrentHashMap<>();

    @Override
    public ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal) {
        return profiles.compute(userKey, (ignored, existing) -> {
            if (existing == null) {
                onboardingSteps.put(userKey, OnboardingStep.PREFERENCES);
                return new ProfileSummary(
                        primaryGoal,
                        20,
                        CorrectionStyle.STANDARD,
                        false,
                        RawContentRetention.STORE,
                        RawContentRetention.STORE,
                        0,
                        false,
                        1);
            }
            long nextVersion = existing.primaryGoal() == primaryGoal
                    ? existing.profileVersion()
                    : existing.profileVersion() + 1;
            onboardingSteps.put(userKey, OnboardingStep.PREFERENCES);
            return new ProfileSummary(
                    primaryGoal,
                    existing.dailyMinutes(),
                    existing.correctionStyle(),
                    existing.reminderEnabled(),
                    existing.rawTextRetention(),
                    existing.rawAudioRetention(),
                    existing.rawAudioRetentionDays(),
                    false,
                    nextVersion);
        });
    }

    @Override
    public ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences) {
        ProfileSummary existing = requireProfile(userKey);
        long nextVersion = preferencesChanged(existing, preferences)
                ? existing.profileVersion() + 1
                : existing.profileVersion();
        ProfileSummary summary = new ProfileSummary(
                existing.primaryGoal(),
                preferences.dailyMinutes(),
                preferences.correctionStyle(),
                preferences.reminderEnabled(),
                preferences.rawTextRetention(),
                preferences.rawAudioRetention(),
                existing.rawAudioRetentionDays(),
                false,
                nextVersion);
        profiles.put(userKey, summary);
        onboardingSteps.put(userKey, OnboardingStep.SELF_ASSESSMENT);
        return summary;
    }

    @Override
    public PrivacySettings getPrivacySettings(UserKey userKey) {
        ProfileSummary existing = requireProfile(userKey);
        return new PrivacySettings(
                existing.rawTextRetention(),
                existing.rawAudioRetention(),
                existing.rawAudioRetentionDays());
    }

    @Override
    public PrivacySettings savePrivacySettings(UserKey userKey, PrivacySettings privacySettings) {
        ProfileSummary existing = requireProfile(userKey);
        long nextVersion = privacyChanged(existing, privacySettings)
                ? existing.profileVersion() + 1
                : existing.profileVersion();
        profiles.put(userKey, new ProfileSummary(
                existing.primaryGoal(),
                existing.dailyMinutes(),
                existing.correctionStyle(),
                existing.reminderEnabled(),
                privacySettings.rawTextRetention(),
                privacySettings.rawAudioRetention(),
                privacySettings.rawAudioRetentionDays(),
                existing.onboardingCompleted(),
                nextVersion));
        return privacySettings;
    }

    @Override
    public void advanceOnboardingToAssessment(UserKey userKey) {
        requireProfile(userKey);
        onboardingSteps.put(userKey, OnboardingStep.ASSESSMENT);
    }

    @Override
    public void advanceOnboardingToResult(UserKey userKey) {
        requireProfile(userKey);
        onboardingSteps.put(userKey, OnboardingStep.RESULT);
    }

    @Override
    public OnboardingProgress getOnboardingProgress(UserKey userKey) {
        ProfileSummary existing = profiles.get(userKey);
        return OnboardingProgress.recover(
                existing != null,
                existing != null && existing.primaryGoal() != null,
                onboardingSteps.getOrDefault(userKey, OnboardingStep.GOAL).name(),
                null);
    }

    private ProfileSummary requireProfile(UserKey userKey) {
        ProfileSummary existing = profiles.get(userKey);
        if (existing == null) {
            throw new IllegalArgumentException("primary goal must be saved before preferences");
        }
        return existing;
    }

    private boolean preferencesChanged(ProfileSummary existing, LearningPreferences preferences) {
        return existing.dailyMinutes() != preferences.dailyMinutes()
                || existing.correctionStyle() != preferences.correctionStyle()
                || existing.reminderEnabled() != preferences.reminderEnabled()
                || existing.rawTextRetention() != preferences.rawTextRetention()
                || existing.rawAudioRetention() != preferences.rawAudioRetention();
    }

    private boolean privacyChanged(ProfileSummary existing, PrivacySettings privacySettings) {
        return existing.rawTextRetention() != privacySettings.rawTextRetention()
                || existing.rawAudioRetention() != privacySettings.rawAudioRetention()
                || existing.rawAudioRetentionDays() != privacySettings.rawAudioRetentionDays();
    }
}
