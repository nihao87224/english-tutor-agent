package cn.forever24.tutor.ai.fake;

import cn.forever24.tutor.ai.provider.AsrOptions;
import cn.forever24.tutor.ai.provider.AsrResult;
import cn.forever24.tutor.ai.provider.AudioInput;
import cn.forever24.tutor.ai.provider.ProviderCapabilities;
import cn.forever24.tutor.ai.provider.ProviderCapability;
import cn.forever24.tutor.ai.provider.ProviderTrace;
import cn.forever24.tutor.ai.provider.ProviderUsage;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;

import java.time.Duration;
import java.util.Set;

import static cn.forever24.tutor.ai.provider.ProviderText.requireNonNull;

public class FakeSpeechToTextProvider implements SpeechToTextProvider {

    private static final ProviderCapabilities CAPABILITIES = new ProviderCapabilities(
            FakeProviderVersions.PROVIDER_ID,
            FakeProviderVersions.ASR_MODEL_ID,
            Set.of(ProviderCapability.SPEECH_TO_TEXT),
            Duration.ofSeconds(2)
    );

    @Override
    public AsrResult transcribe(AudioInput input, AsrOptions options) {
        AudioInput safeInput = requireNonNull(input, "input");
        requireNonNull(options, "options");
        return new AsrResult(
                "This is a deterministic fake transcription for local development.",
                0.97,
                new ProviderTrace(
                        safeInput.traceId(),
                        FakeProviderVersions.PROVIDER_ID,
                        FakeProviderVersions.ASR_MODEL_ID,
                        FakeProviderVersions.PROMPT_VERSION,
                        FakeProviderVersions.SCHEMA_VERSION
                ),
                ProviderUsage.freeAudio(safeInput.duration().toMillis(), 0)
        );
    }

    @Override
    public ProviderCapabilities capabilities() {
        return CAPABILITIES;
    }
}
