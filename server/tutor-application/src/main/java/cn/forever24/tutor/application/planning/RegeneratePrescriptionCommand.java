package cn.forever24.tutor.application.planning;

public record RegeneratePrescriptionCommand(
        String currentPrescriptionId,
        long currentVersion,
        PrescriptionFeedbackReason reason,
        Integer availableMinutes,
        String temporaryGoal,
        String note
) {

    public RegeneratePrescriptionCommand {
        if (currentPrescriptionId == null || currentPrescriptionId.isBlank() || currentVersion < 1) {
            throw new IllegalArgumentException("current prescription id and version are required");
        }
        currentPrescriptionId = currentPrescriptionId.strip();
        if (reason == null || reason == PrescriptionFeedbackReason.BLOCK_SKIPPED) {
            throw new IllegalArgumentException("valid regeneration reason is required");
        }
    }

    public PrescriptionFeedback feedback() {
        return new PrescriptionFeedback(reason, availableMinutes, temporaryGoal, note, null);
    }
}
