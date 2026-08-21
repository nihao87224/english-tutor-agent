package cn.forever24.tutor.application.training;

public record ConfirmTranscriptCommand(TranscriptConfirmationDecision decision, String correctedText) {
    public ConfirmTranscriptCommand {
        if (decision == null) throw new IllegalArgumentException("decision is required");
        correctedText = correctedText == null ? null : correctedText.strip();
        if (decision == TranscriptConfirmationDecision.CORRECT
                && (correctedText == null || correctedText.isBlank())) {
            throw new IllegalArgumentException("correctedText is required for CORRECT");
        }
        if (correctedText != null && correctedText.length() > 4000) {
            throw new IllegalArgumentException("correctedText must not exceed 4000 characters");
        }
    }
}
