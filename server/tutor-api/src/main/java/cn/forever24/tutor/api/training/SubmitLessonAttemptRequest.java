package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.SubmitLessonAttemptCommand;
import cn.forever24.tutor.training.TaskAttemptInputType;

import java.time.Instant;

public record SubmitLessonAttemptRequest(
        String taskId,
        TaskAttemptInputType inputType,
        String text,
        String audioAssetId,
        String retryOfAttemptId,
        Instant clientStartedAt,
        Integer clientDurationMs
) {
    SubmitLessonAttemptCommand toCommand() {
        return new SubmitLessonAttemptCommand(
                taskId, inputType, text, audioAssetId, retryOfAttemptId, clientStartedAt, clientDurationMs);
    }
}
