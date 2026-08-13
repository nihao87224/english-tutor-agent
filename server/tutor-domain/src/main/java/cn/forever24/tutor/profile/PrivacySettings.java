package cn.forever24.tutor.profile;

public record PrivacySettings(
        RawContentRetention rawTextRetention,
        RawContentRetention rawAudioRetention,
        int rawAudioRetentionDays
) {
    public PrivacySettings {
        if (rawTextRetention == null) {
            throw new IllegalArgumentException("saveRawText is required");
        }
        if (rawAudioRetention == null) {
            throw new IllegalArgumentException("saveRawAudio is required");
        }
        if (rawAudioRetentionDays < 0 || rawAudioRetentionDays > 365) {
            throw new IllegalArgumentException("rawAudioRetentionDays must be between 0 and 365");
        }
    }

    public static PrivacySettings fromContractValues(
            Boolean saveRawText,
            Boolean saveRawAudio,
            Integer rawAudioRetentionDays
    ) {
        return new PrivacySettings(
                RawContentRetention.fromSaveFlag(saveRawText, "saveRawText"),
                RawContentRetention.fromSaveFlag(saveRawAudio, "saveRawAudio"),
                rawAudioRetentionDays == null ? 0 : rawAudioRetentionDays);
    }

    public boolean saveRawText() {
        return rawTextRetention.savesRawContent();
    }

    public boolean saveRawAudio() {
        return rawAudioRetention.savesRawContent();
    }
}
