package cn.forever24.tutor.api.profile;

import cn.forever24.tutor.profile.PrivacySettings;

public record PrivacySettingsResponse(
        boolean saveRawText,
        boolean saveRawAudio,
        int rawAudioRetentionDays
) {

    public static PrivacySettingsResponse from(PrivacySettings settings) {
        return new PrivacySettingsResponse(
                settings.saveRawText(),
                settings.saveRawAudio(),
                settings.rawAudioRetentionDays());
    }
}
