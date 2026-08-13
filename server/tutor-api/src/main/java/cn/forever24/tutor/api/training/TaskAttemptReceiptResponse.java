package cn.forever24.tutor.api.training;

import cn.forever24.tutor.training.TaskAttemptReceipt;

public record TaskAttemptReceiptResponse(
        String attemptId,
        String status,
        boolean feedbackAvailable,
        int evidenceCount
) {

    static TaskAttemptReceiptResponse from(TaskAttemptReceipt receipt) {
        return new TaskAttemptReceiptResponse(
                receipt.attemptId(),
                receipt.status().name(),
                receipt.feedbackAvailable(),
                receipt.evidenceCount());
    }
}
