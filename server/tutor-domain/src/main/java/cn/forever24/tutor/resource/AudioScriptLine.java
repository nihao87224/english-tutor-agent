package cn.forever24.tutor.resource;

public record AudioScriptLine(String sentenceId, String speaker, String text) {

    public AudioScriptLine {
        sentenceId = ResourceValidation.required(sentenceId, "sentenceId");
        speaker = ResourceValidation.required(speaker, "speaker");
        text = ResourceValidation.required(text, "text");
    }
}
