package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.PrescriptionApplicationException;
import cn.forever24.tutor.application.planning.PrescriptionFeedback;
import cn.forever24.tutor.application.planning.PrescriptionMutationResult;
import cn.forever24.tutor.application.planning.PrescriptionRepository;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.profile.UserKey;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPrescriptionRepository implements PrescriptionRepository {

    private final Map<String, DailyLearningPrescription> prescriptions = new ConcurrentHashMap<>();
    private final Map<String, String> activeByUserDate = new ConcurrentHashMap<>();
    private final Map<String, Replay> replays = new ConcurrentHashMap<>();
    private final Object monitor = new Object();

    @Override
    public Optional<DailyLearningPrescription> findActive(UserKey userKey, LocalDate learningDate) {
        String prescriptionId = activeByUserDate.get(userDate(userKey, learningDate));
        return Optional.ofNullable(prescriptionId).map(prescriptions::get);
    }

    @Override
    public Optional<DailyLearningPrescription> findOwned(UserKey userKey, String prescriptionId) {
        DailyLearningPrescription prescription = prescriptions.get(prescriptionId);
        return prescription != null && prescription.userKey().equals(userKey)
                ? Optional.of(prescription)
                : Optional.empty();
    }

    @Override
    public Optional<DailyLearningPrescription> findOwnedForUpdate(UserKey userKey, String prescriptionId) {
        synchronized (monitor) {
            return findOwned(userKey, prescriptionId);
        }
    }

    @Override
    public Optional<PrescriptionMutationResult> findReplay(
            UserKey userKey,
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        return Optional.ofNullable(replay(replayKey(userKey, operation, idempotencyKey), requestHash));
    }

    @Override
    public DailyLearningPrescription saveInitialIfAbsent(DailyLearningPrescription prescription) {
        synchronized (monitor) {
            String activeKey = userDate(prescription.userKey(), prescription.learningDate());
            String existingId = activeByUserDate.get(activeKey);
            if (existingId != null) {
                return prescriptions.get(existingId);
            }
            prescriptions.put(prescription.prescriptionId(), prescription);
            activeByUserDate.put(activeKey, prescription.prescriptionId());
            return prescription;
        }
    }

    @Override
    public PrescriptionMutationResult replaceActive(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription replacement,
            PrescriptionFeedback feedback,
            String idempotencyKey,
            String requestHash
    ) {
        synchronized (monitor) {
            String replayKey = replayKey(expectedCurrent.userKey(), "REGENERATE", idempotencyKey);
            PrescriptionMutationResult replay = replay(replayKey, requestHash);
            if (replay != null) {
                return replay;
            }
            String activeKey = userDate(expectedCurrent.userKey(), expectedCurrent.learningDate());
            if (!expectedCurrent.prescriptionId().equals(activeByUserDate.get(activeKey))) {
                throw stale();
            }
            prescriptions.put(expectedCurrent.prescriptionId(), expectedCurrent.superseded());
            prescriptions.put(replacement.prescriptionId(), replacement);
            activeByUserDate.put(activeKey, replacement.prescriptionId());
            replays.put(replayKey, new Replay(requestHash, replacement.prescriptionId()));
            return new PrescriptionMutationResult(replacement, false);
        }
    }

    @Override
    public PrescriptionMutationResult saveBlockSkip(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription updated,
            PrescriptionFeedback feedback,
            String idempotencyKey,
            String requestHash
    ) {
        synchronized (monitor) {
            String replayKey = replayKey(expectedCurrent.userKey(), "SKIP", idempotencyKey);
            PrescriptionMutationResult replay = replay(replayKey, requestHash);
            if (replay != null) {
                return replay;
            }
            String activeKey = userDate(expectedCurrent.userKey(), expectedCurrent.learningDate());
            if (!expectedCurrent.prescriptionId().equals(activeByUserDate.get(activeKey))) {
                throw stale();
            }
            prescriptions.put(updated.prescriptionId(), updated);
            replays.put(replayKey, new Replay(requestHash, updated.prescriptionId()));
            return new PrescriptionMutationResult(updated, false);
        }
    }

    private PrescriptionMutationResult replay(String replayKey, String requestHash) {
        Replay replay = replays.get(replayKey);
        if (replay == null) {
            return null;
        }
        if (!replay.requestHash().equals(requestHash)) {
            throw new PrescriptionApplicationException(
                    "IDEMPOTENCY_CONFLICT", 409, "Idempotency-Key was already used for another request");
        }
        return new PrescriptionMutationResult(prescriptions.get(replay.prescriptionId()), true);
    }

    private static PrescriptionApplicationException stale() {
        return new PrescriptionApplicationException(
                "PRESCRIPTION_STALE", 409, "prescription is no longer active");
    }

    private static String userDate(UserKey userKey, LocalDate date) {
        return userKey.value() + "|" + date;
    }

    private static String replayKey(UserKey userKey, String operation, String idempotencyKey) {
        return userKey.value() + "|" + operation + "|" + idempotencyKey;
    }

    private record Replay(String requestHash, String prescriptionId) {
    }
}
