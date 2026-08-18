package cn.forever24.tutor.assessment;

import java.util.List;

/** A delivery-safe initial assessment item. Correct answers remain in the item banks. */
public record AssessmentItem(
        String itemId,
        String skill,
        String type,
        String prompt,
        List<String> options,
        Integer timeLimitSeconds
) {
    public AssessmentItem {
        if (itemId == null || itemId.isBlank() || skill == null || skill.isBlank()
                || type == null || type.isBlank() || prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("assessment item fields are required");
        }
        options = options == null ? List.of() : List.copyOf(options);
    }
}
