package cn.forever24.tutor.api.profile;

public record PrivacySettingsRequest(
        Boolean saveRawText,
        Boolean saveRawAudio,
        Integer rawAudioRetentionDays
) {
}
