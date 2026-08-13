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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class JdbcUserProfileRepository implements UserProfileRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcUserProfileRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        long userId = ensureUser(userKey, now);
        ExistingProfile existingProfile = findExistingProfile(userId);
        if (existingProfile == null) {
            jdbcTemplate.update("""
                            INSERT INTO user_learning_profile
                                (user_id, primary_goal, onboarding_status, profile_version,
                                 created_at_utc, updated_at_utc, version)
                            VALUES (?, ?, 'PREFERENCES', 1, ?, ?, 0)
                            """,
                    userId,
                    primaryGoal.name(),
                    now,
                    now);
        } else if (existingProfile.primaryGoal() != primaryGoal) {
            jdbcTemplate.update("""
                            UPDATE user_learning_profile
                            SET primary_goal = ?,
                                onboarding_status = 'PREFERENCES',
                                profile_version = profile_version + 1,
                                updated_at_utc = ?,
                                version = version + 1
                            WHERE user_id = ?
                            """,
                    primaryGoal.name(),
                    now,
                    userId);
        }
        return getProfileSummary(userId);
    }

    @Override
    public ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Long userId = findUserId(userKey);
        if (userId == null) {
            throw new IllegalArgumentException("primary goal must be saved before preferences");
        }
        ExistingProfile existingProfile = findExistingProfile(userId);
        if (existingProfile == null || existingProfile.primaryGoal() == null) {
            throw new IllegalArgumentException("primary goal must be saved before preferences");
        }

        boolean valuesChanged = preferencesChanged(existingProfile, preferences);
        jdbcTemplate.update("""
                        UPDATE user_learning_profile
                        SET daily_minutes = ?,
                            correction_preference = ?,
                            reminder_enabled = ?,
                            raw_text_retention = ?,
                            raw_audio_retention = ?,
                            onboarding_status = 'SELF_ASSESSMENT',
                            profile_version = profile_version + ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ?
                        """,
                preferences.dailyMinutes(),
                preferences.correctionStyle().name(),
                preferences.reminderEnabled(),
                preferences.rawTextRetention().name(),
                preferences.rawAudioRetention().name(),
                valuesChanged ? 1 : 0,
                now,
                userId);
        return getProfileSummary(userId);
    }

    @Override
    public PrivacySettings getPrivacySettings(UserKey userKey) {
        Long userId = findUserId(userKey);
        if (userId == null || findExistingProfile(userId) == null) {
            throw new IllegalArgumentException("profile must exist before privacy settings can be read");
        }
        return getPrivacySettings(userId);
    }

    @Override
    public PrivacySettings savePrivacySettings(UserKey userKey, PrivacySettings privacySettings) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Long userId = findUserId(userKey);
        if (userId == null) {
            throw new IllegalArgumentException("profile must exist before privacy settings can be saved");
        }
        ExistingProfile existingProfile = findExistingProfile(userId);
        if (existingProfile == null) {
            throw new IllegalArgumentException("profile must exist before privacy settings can be saved");
        }

        boolean valuesChanged = privacyChanged(existingProfile, privacySettings);
        jdbcTemplate.update("""
                        UPDATE user_learning_profile
                        SET raw_text_retention = ?,
                            raw_audio_retention = ?,
                            raw_audio_retention_days = ?,
                            profile_version = profile_version + ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ?
                        """,
                privacySettings.rawTextRetention().name(),
                privacySettings.rawAudioRetention().name(),
                privacySettings.rawAudioRetentionDays(),
                valuesChanged ? 1 : 0,
                now,
                userId);
        return getPrivacySettings(userId);
    }

    @Override
    public void advanceOnboardingToAssessment(UserKey userKey) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Long userId = findUserId(userKey);
        if (userId == null || findExistingProfile(userId) == null) {
            throw new IllegalArgumentException("profile must exist before self assessment");
        }
        jdbcTemplate.update("""
                        UPDATE user_learning_profile
                        SET onboarding_status = 'ASSESSMENT',
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ?
                        """,
                now,
                userId);
    }

    @Override
    public void advanceOnboardingToResult(UserKey userKey) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Long userId = findUserId(userKey);
        if (userId == null || findExistingProfile(userId) == null) {
            throw new IllegalArgumentException("profile must exist before assessment result");
        }
        jdbcTemplate.update("""
                        UPDATE user_learning_profile
                        SET onboarding_status = 'RESULT',
                            profile_version = profile_version + 1,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE user_id = ?
                          AND onboarding_status <> 'RESULT'
                        """,
                now,
                userId);
    }

    @Override
    public OnboardingProgress getOnboardingProgress(UserKey userKey) {
        Long userId = findUserId(userKey);
        if (userId == null) {
            return OnboardingProgress.recover(false, false, null, null);
        }
        ExistingProfile existingProfile = findExistingProfile(userId);
        return OnboardingProgress.recover(
                existingProfile != null,
                existingProfile != null && existingProfile.primaryGoal() != null,
                existingProfile == null ? null : existingProfile.onboardingStatus(),
                null);
    }

    private long ensureUser(UserKey userKey, LocalDateTime now) {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO app_user
                            (user_key, status, timezone, locale, created_at_utc, updated_at_utc, version)
                        VALUES (?, 'ACTIVE', 'Asia/Shanghai', 'zh-CN', ?, ?, 0)
                        """,
                userKey.value(),
                now,
                now);
        return findUserId(userKey);
    }

    private Long findUserId(UserKey userKey) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE'",
                    Long.class,
                    userKey.value());
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private ExistingProfile findExistingProfile(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT primary_goal, daily_minutes, correction_preference,
                                   reminder_enabled, raw_text_retention, raw_audio_retention,
                                   raw_audio_retention_days, onboarding_status, profile_version
                            FROM user_learning_profile
                            WHERE user_id = ?
                            """,
                    (resultSet, rowNum) -> mapExistingProfile(resultSet),
                    userId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private ProfileSummary getProfileSummary(long userId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT primary_goal, daily_minutes, correction_preference,
                               reminder_enabled, raw_text_retention, raw_audio_retention,
                               raw_audio_retention_days, onboarding_status, profile_version
                        FROM user_learning_profile
                        WHERE user_id = ?
                        """,
                (resultSet, rowNum) -> new ProfileSummary(
                        readPrimaryGoal(resultSet),
                        resultSet.getInt("daily_minutes"),
                        CorrectionStyle.valueOf(resultSet.getString("correction_preference")),
                        resultSet.getBoolean("reminder_enabled"),
                        RawContentRetention.valueOf(resultSet.getString("raw_text_retention")),
                        RawContentRetention.valueOf(resultSet.getString("raw_audio_retention")),
                        resultSet.getInt("raw_audio_retention_days"),
                        "COMPLETE".equals(resultSet.getString("onboarding_status")),
                        resultSet.getLong("profile_version")),
                userId);
    }

    private ExistingProfile mapExistingProfile(ResultSet resultSet) throws SQLException {
        return new ExistingProfile(
                readPrimaryGoal(resultSet),
                resultSet.getInt("daily_minutes"),
                CorrectionStyle.valueOf(resultSet.getString("correction_preference")),
                resultSet.getBoolean("reminder_enabled"),
                RawContentRetention.valueOf(resultSet.getString("raw_text_retention")),
                RawContentRetention.valueOf(resultSet.getString("raw_audio_retention")),
                resultSet.getInt("raw_audio_retention_days"),
                resultSet.getString("onboarding_status"),
                resultSet.getLong("profile_version"));
    }

    private PrimaryGoal readPrimaryGoal(ResultSet resultSet) throws SQLException {
        String primaryGoal = resultSet.getString("primary_goal");
        if (primaryGoal == null || primaryGoal.isBlank()) {
            return null;
        }
        return PrimaryGoal.valueOf(primaryGoal);
    }

    private PrivacySettings getPrivacySettings(long userId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT raw_text_retention, raw_audio_retention, raw_audio_retention_days
                        FROM user_learning_profile
                        WHERE user_id = ?
                        """,
                (resultSet, rowNum) -> new PrivacySettings(
                        RawContentRetention.valueOf(resultSet.getString("raw_text_retention")),
                        RawContentRetention.valueOf(resultSet.getString("raw_audio_retention")),
                        resultSet.getInt("raw_audio_retention_days")),
                userId);
    }

    private boolean preferencesChanged(ExistingProfile existingProfile, LearningPreferences preferences) {
        return existingProfile.dailyMinutes() != preferences.dailyMinutes()
                || existingProfile.correctionPreference() != preferences.correctionStyle()
                || existingProfile.reminderEnabled() != preferences.reminderEnabled()
                || existingProfile.rawTextRetention() != preferences.rawTextRetention()
                || existingProfile.rawAudioRetention() != preferences.rawAudioRetention();
    }

    private boolean privacyChanged(ExistingProfile existingProfile, PrivacySettings privacySettings) {
        return existingProfile.rawTextRetention() != privacySettings.rawTextRetention()
                || existingProfile.rawAudioRetention() != privacySettings.rawAudioRetention()
                || existingProfile.rawAudioRetentionDays() != privacySettings.rawAudioRetentionDays();
    }

    private record ExistingProfile(
            PrimaryGoal primaryGoal,
            int dailyMinutes,
            CorrectionStyle correctionPreference,
            boolean reminderEnabled,
            RawContentRetention rawTextRetention,
            RawContentRetention rawAudioRetention,
            int rawAudioRetentionDays,
            String onboardingStatus,
            long profileVersion
    ) {
    }
}
