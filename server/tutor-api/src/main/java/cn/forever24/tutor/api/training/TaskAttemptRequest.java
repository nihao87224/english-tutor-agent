package cn.forever24.tutor.api.training;

import java.time.Instant;

public record TaskAttemptRequest(
        String inputType,
        String text,
        String audioAssetId,
        String option,
        Integer hintLevel,
        Integer clientDurationMs,
        Instant clientStartedAt,
        Instant clientCompletedAt
) {
}
