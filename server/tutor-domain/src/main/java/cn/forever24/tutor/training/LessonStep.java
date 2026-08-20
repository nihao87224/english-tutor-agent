package cn.forever24.tutor.training;

public enum LessonStep {
    SCENE_CONTEXT(CompletionMode.CLIENT_ACKNOWLEDGEMENT),
    FIRST_LISTEN(CompletionMode.CLIENT_ACKNOWLEDGEMENT),
    COMPREHENSION(CompletionMode.ATTEMPT_REQUIRED),
    TRANSCRIPT_EXPRESSIONS(CompletionMode.CLIENT_ACKNOWLEDGEMENT),
    GUIDED_SPEAKING(CompletionMode.ATTEMPT_REQUIRED),
    ROLE_PLAY(CompletionMode.ATTEMPT_REQUIRED),
    FEEDBACK(CompletionMode.SYSTEM),
    RETRY(CompletionMode.ATTEMPT_REQUIRED),
    EVIDENCE(CompletionMode.SYSTEM),
    COMPLETE(CompletionMode.SYSTEM);

    private final CompletionMode completionMode;

    LessonStep(CompletionMode completionMode) {
        this.completionMode = completionMode;
    }

    public CompletionMode completionMode() {
        return completionMode;
    }

    public boolean clientCompletable() {
        return completionMode == CompletionMode.CLIENT_ACKNOWLEDGEMENT;
    }

    public enum CompletionMode {
        CLIENT_ACKNOWLEDGEMENT,
        ATTEMPT_REQUIRED,
        SYSTEM
    }
}
