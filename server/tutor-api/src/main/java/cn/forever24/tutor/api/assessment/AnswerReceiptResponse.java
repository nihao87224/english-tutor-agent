package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.assessment.AssessmentAnswerReceipt;

public record AnswerReceiptResponse(
        String answerId,
        boolean accepted
) {

    public static AnswerReceiptResponse from(AssessmentAnswerReceipt receipt) {
        return new AnswerReceiptResponse(receipt.answerId(), receipt.accepted());
    }
}
