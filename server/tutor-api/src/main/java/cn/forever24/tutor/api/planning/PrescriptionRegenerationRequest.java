package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.application.planning.PrescriptionFeedbackReason;
import cn.forever24.tutor.application.planning.RegeneratePrescriptionCommand;

public record PrescriptionRegenerationRequest(
        String currentPrescriptionId,
        long currentVersion,
        PrescriptionFeedbackReason reason,
        Integer availableMinutes,
        String temporaryGoal,
        String note
) {

    RegeneratePrescriptionCommand toCommand() {
        return new RegeneratePrescriptionCommand(
                currentPrescriptionId,
                currentVersion,
                reason,
                availableMinutes,
                temporaryGoal,
                note);
    }
}
