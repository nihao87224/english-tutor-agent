package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.planning.DailyLearningPrescription;

public record PrescriptionMutationResult(DailyLearningPrescription prescription, boolean replayed) {

    public PrescriptionMutationResult {
        if (prescription == null) {
            throw new IllegalArgumentException("prescription is required");
        }
    }
}
