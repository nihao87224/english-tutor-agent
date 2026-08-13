package cn.forever24.tutor.profile;

import java.util.Set;

public record LearningPreferences(
        int dailyMinutes,
        CorrectionStyle correctionStyle,
        boolean reminderEnabled,
        RawContentRetention rawTextRetention,
        RawContentRetention rawAudioRetention
) {
    private static final Set<Integer> ALLOWED_DAILY_MINUTES = Set.of(5, 10, 20, 30, 45);

    public LearningPreferences {
        if (!ALLOWED_DAILY_MINUTES.contains(dailyMinutes)) {
            throw new IllegalArgumentException("unsupported daily minutes: " + dailyMinutes);
        }
        if (correctionStyle == null) {
            throw new IllegalArgumentException("correction style is required");
        }
        if (rawTextRetention == null) {
            throw new IllegalArgumentException("saveRawText is required");
        }
        if (rawAudioRetention == null) {
            throw new IllegalArgumentException("saveRawAudio is required");
        }
    }

    public static LearningPreferences fromContractValues(
            Integer dailyMinutes,
            String correctionStyle,
            Boolean reminderEnabled,
            Boolean saveRawText,
            Boolean saveRawAudio
    ) {
        if (dailyMinutes == null) {
            throw new IllegalArgumentException("daily minutes is required");
        }
        if (reminderEnabled == null) {
            throw new IllegalArgumentException("reminderEnabled is required");
        }
        return new LearningPreferences(
                dailyMinutes,
                CorrectionStyle.fromContractValue(correctionStyle),
                reminderEnabled,
                RawContentRetention.fromSaveFlag(saveRawText, "saveRawText"),
                RawContentRetention.fromSaveFlag(saveRawAudio, "saveRawAudio"));
    }

    public boolean saveRawText() {
        return rawTextRetention.savesRawContent();
    }

    public boolean saveRawAudio() {
        return rawAudioRetention.savesRawContent();
    }
}
