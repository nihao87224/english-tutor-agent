package cn.forever24.tutor.ai.fake;

import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.ai.provider.TtsResult;
import cn.forever24.tutor.ai.provider.VoiceOptions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import static cn.forever24.tutor.ai.provider.ProviderText.requireNonBlank;
import static cn.forever24.tutor.ai.provider.ProviderText.requireNonNull;

public class FakeTextToSpeechProvider implements TextToSpeechProvider {

    private static final ProviderCapabilities CAPABILITIES = new ProviderCapabilities(
            FakeProviderVersions.PROVIDER_ID,
            FakeProviderVersions.TTS_MODEL_ID,
            Set.of(ProviderCapability.TEXT_TO_SPEECH),
            Duration.ofSeconds(2)
    );

    @Override
    public TtsResult synthesize(String traceId, String text, VoiceOptions options) {
        String safeTraceId = requireNonBlank(traceId, "traceId");
        String safeText = requireNonBlank(text, "text");
        VoiceOptions safeOptions = requireNonNull(options, "options");
        byte[] audio = ("FAKE_WAV:" + safeText).getBytes(StandardCharsets.UTF_8);
        return new TtsResult(
                audio,
                safeOptions.audioFormat(),
                Duration.ofMillis(Math.max(300, safeText.length() * 35L)),
                new ProviderTrace(
                        safeTraceId,
                        FakeProviderVersions.PROVIDER_ID,
                        FakeProviderVersions.TTS_MODEL_ID,
                        FakeProviderVersions.PROMPT_VERSION,
                        FakeProviderVersions.SCHEMA_VERSION
                ),
                ProviderUsage.freeAudio(0, Math.max(300, safeText.length() * 35L))
        );
    }

    @Override
    public ProviderCapabilities capabilities() {
        return CAPABILITIES;
    }
}
