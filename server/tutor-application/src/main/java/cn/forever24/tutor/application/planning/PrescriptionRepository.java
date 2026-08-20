package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.profile.UserKey;

import java.time.LocalDate;
import java.util.Optional;

public interface PrescriptionRepository {

    Optional<DailyLearningPrescription> findActive(UserKey userKey, LocalDate learningDate);

    Optional<DailyLearningPrescription> findOwned(UserKey userKey, String prescriptionId);

    default Optional<DailyLearningPrescription> findOwnedForUpdate(UserKey userKey, String prescriptionId) {
        return findOwned(userKey, prescriptionId);
    }

    Optional<PrescriptionMutationResult> findReplay(
            UserKey userKey,
            String operation,
            String idempotencyKey,
            String requestHash
    );

    DailyLearningPrescription saveInitialIfAbsent(DailyLearningPrescription prescription);

    PrescriptionMutationResult replaceActive(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription replacement,
            PrescriptionFeedback feedback,
            String idempotencyKey,
            String requestHash
    );

    PrescriptionMutationResult saveBlockSkip(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription updated,
            PrescriptionFeedback feedback,
            String idempotencyKey,
            String requestHash
    );
}
