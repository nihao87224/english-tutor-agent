package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.LessonInputMode;

public record StartLessonSessionCommand(
        String prescriptionId,
        int prescriptionVersion,
        String blockId,
        LessonInputMode inputMode
) {
    public StartLessonSessionCommand {
        if (prescriptionId == null || prescriptionId.isBlank()) {
            throw new IllegalArgumentException("prescriptionId is required");
        }
        if (prescriptionVersion < 1) {
            throw new IllegalArgumentException("prescriptionVersion must be positive");
        }
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("blockId is required");
        }
        inputMode = inputMode == null ? LessonInputMode.VOICE_OR_TEXT : inputMode;
    }
}
