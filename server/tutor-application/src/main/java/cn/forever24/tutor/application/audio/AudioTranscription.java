package cn.forever24.tutor.application.audio;

public record AudioTranscription(String text, double confidence) {
    public AudioTranscription {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("transcription text is required");
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("confidence must be between 0 and 1");
        text = text.strip();
    }
}
