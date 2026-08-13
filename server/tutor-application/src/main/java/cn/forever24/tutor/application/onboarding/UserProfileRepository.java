package cn.forever24.tutor.application.onboarding;

import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.UserKey;

public interface UserProfileRepository {

    ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal);

    ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences);

    PrivacySettings getPrivacySettings(UserKey userKey);

    PrivacySettings savePrivacySettings(UserKey userKey, PrivacySettings privacySettings);

    void advanceOnboardingToAssessment(UserKey userKey);

    void advanceOnboardingToResult(UserKey userKey);

    OnboardingProgress getOnboardingProgress(UserKey userKey);
}
