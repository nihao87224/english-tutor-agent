package cn.forever24.tutor.ai.provider;

public interface TextToSpeechProvider {

    TtsResult synthesize(String traceId, String text, VoiceOptions options);

    ProviderCapabilities capabilities();
}
