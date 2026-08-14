package cn.forever24.tutor.ai.provider;

public record VoiceOptions(
        String voiceId,
        String languageTag,
        String audioFormat
) {

    public VoiceOptions {
        voiceId = ProviderText.requireNonBlank(voiceId, "voiceId");
        languageTag = ProviderText.requireNonBlank(languageTag, "languageTag");
        audioFormat = ProviderText.requireNonBlank(audioFormat, "audioFormat");
    }

    public static VoiceOptions englishPcm() {
        return new VoiceOptions("english-neutral", "en-US", "audio/wav");
    }
}
