package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.assessment.AssessmentItem;

import java.util.List;

public record AssessmentItemResponse(
        String itemId,
        String skill,
        String type,
        String prompt,
        List<String> options,
        Integer timeLimitSeconds
) {
    public static AssessmentItemResponse from(AssessmentItem item) {
        return new AssessmentItemResponse(
                item.itemId(), item.skill(), item.type(), item.prompt(), item.options(), item.timeLimitSeconds());
    }
}
