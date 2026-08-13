package cn.forever24.tutor.training;

public record TaskAttemptReceipt(
        String attemptId,
        TaskAttemptStatus status,
        boolean feedbackAvailable,
        int evidenceCount
) {

    public TaskAttemptReceipt {
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("attemptId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (evidenceCount < 0) {
            throw new IllegalArgumentException("evidenceCount must not be negative");
        }
    }

    public static TaskAttemptReceipt accepted(String attemptId) {
        return accepted(attemptId, 0);
    }

    public static TaskAttemptReceipt accepted(String attemptId, int evidenceCount) {
        return new TaskAttemptReceipt(attemptId, TaskAttemptStatus.ACCEPTED, false, evidenceCount);
    }
}
