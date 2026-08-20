package cn.forever24.tutor.resource;

import java.util.List;

public record AudioAssetMetadata(
        String generationPrompt,
        AssetGenerationMetadata generation,
        String speakerRole,
        String voiceId,
        String accent,
        double speechRate,
        String transcriptRef,
        List<AudioScriptLine> audioScript
) implements AssetMetadata {

    public AudioAssetMetadata {
        generationPrompt = ResourceValidation.required(generationPrompt, "generationPrompt");
        if (generationPrompt.length() < 20) {
            throw new IllegalArgumentException("generationPrompt must contain at least 20 characters");
        }
        if (generation == null) {
            throw new IllegalArgumentException("generation metadata is required");
        }
        speakerRole = ResourceValidation.required(speakerRole, "speakerRole");
        voiceId = ResourceValidation.required(voiceId, "voiceId");
        accent = ResourceValidation.required(accent, "accent");
        if (speechRate < 0.5 || speechRate > 1.5) {
            throw new IllegalArgumentException("speechRate must be between 0.5 and 1.5");
        }
        transcriptRef = ResourceValidation.required(transcriptRef, "transcriptRef");
        if (audioScript == null || audioScript.isEmpty()) {
            throw new IllegalArgumentException("audioScript is required");
        }
        audioScript = List.copyOf(audioScript);
    }
}
