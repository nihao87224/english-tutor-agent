package cn.forever24.tutor.api.training;

public record StartTrainingSessionRequest(
        String planId,
        String mode
) {
}
