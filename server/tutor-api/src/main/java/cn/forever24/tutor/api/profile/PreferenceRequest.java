package cn.forever24.tutor.api.profile;

public record PreferenceRequest(
        Integer dailyMinutes,
        String correctionStyle,
        Boolean reminderEnabled,
        Boolean saveRawText,
        Boolean saveRawAudio
) {
}
