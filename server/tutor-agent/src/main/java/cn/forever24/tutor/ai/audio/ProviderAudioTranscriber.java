package cn.forever24.tutor.ai.audio;

import cn.forever24.tutor.ai.provider.AiProviderErrorType;
import cn.forever24.tutor.ai.provider.AiProviderException;
import cn.forever24.tutor.ai.provider.AsrOptions;
import cn.forever24.tutor.ai.provider.AudioInput;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.application.audio.AudioTranscriber;
import cn.forever24.tutor.application.audio.AudioTranscription;
import cn.forever24.tutor.application.audio.AudioTranscriptionException;
import cn.forever24.tutor.application.audio.AudioTranscriptionRequest;

public final class ProviderAudioTranscriber implements AudioTranscriber {
    private final SpeechToTextProvider provider;

    public ProviderAudioTranscriber(SpeechToTextProvider provider) {
        this.provider = provider;
    }

    @Override
    public AudioTranscription transcribe(AudioTranscriptionRequest request) {
        try {
            var result = provider.transcribe(
                    new AudioInput(request.traceId(), request.content(), request.mimeType(), request.duration()),
                    AsrOptions.english());
            return new AudioTranscription(result.transcript(), result.confidence());
        } catch (AiProviderException exception) {
            boolean retryable = exception.errorType() == AiProviderErrorType.TIMEOUT
                    || exception.errorType() == AiProviderErrorType.PROVIDER_UNAVAILABLE
                    || exception.errorType() == AiProviderErrorType.UNKNOWN;
            throw new AudioTranscriptionException(retryable, "speech transcription failed", exception);
        } catch (RuntimeException exception) {
            throw new AudioTranscriptionException(true, "speech transcription failed", exception);
        }
    }
}
