package cn.forever24.tutor.application.planning;

public record PrescriptionFeedback(
        PrescriptionFeedbackReason reason,
        Integer availableMinutes,
        String temporaryGoal,
        String note,
        String blockId
) {

    public PrescriptionFeedback {
        if (reason == null) {
            throw new IllegalArgumentException("feedback reason is required");
        }
        if (availableMinutes != null && (availableMinutes < 1 || availableMinutes > 480)) {
            throw new IllegalArgumentException("availableMinutes must be between 1 and 480");
        }
        temporaryGoal = optional(temporaryGoal, 500, "temporaryGoal");
        note = optional(note, 1000, "note");
        blockId = optional(blockId, 64, "blockId");
        if (reason == PrescriptionFeedbackReason.TIME_INSUFFICIENT && availableMinutes == null) {
            throw new IllegalArgumentException("availableMinutes is required for TIME_INSUFFICIENT");
        }
        if (reason == PrescriptionFeedbackReason.TEMPORARY_GOAL && temporaryGoal == null) {
            throw new IllegalArgumentException("temporaryGoal is required for TEMPORARY_GOAL");
        }
        if (reason == PrescriptionFeedbackReason.BLOCK_SKIPPED && blockId == null) {
            throw new IllegalArgumentException("blockId is required for BLOCK_SKIPPED");
        }
    }

    private static String optional(String value, int maximumLength, String field) {
        if (value == null) {
            return null;
        }
        if (value.isBlank() || value.strip().length() > maximumLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value.strip();
    }
}
