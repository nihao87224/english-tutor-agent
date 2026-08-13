package cn.forever24.tutor.ai.provider;

public interface SpeechToTextProvider {

    AsrResult transcribe(AudioInput input, AsrOptions options);

    ProviderCapabilities capabilities();
}
