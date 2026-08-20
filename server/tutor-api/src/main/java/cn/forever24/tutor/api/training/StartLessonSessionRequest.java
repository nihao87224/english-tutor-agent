package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.StartLessonSessionCommand;
import cn.forever24.tutor.training.LessonInputMode;

public record StartLessonSessionRequest(
        String prescriptionId,
        int prescriptionVersion,
        String blockId,
        LessonInputMode inputMode
) {
    StartLessonSessionCommand toCommand() {
        return new StartLessonSessionCommand(prescriptionId, prescriptionVersion, blockId, inputMode);
    }
}
