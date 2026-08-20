package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.entitlement.EntitlementApplicationService;
import cn.forever24.tutor.application.planning.PrescriptionRepository;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessDecisionReason;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.planning.PrescriptionBlockStatus;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.ResourceVersionStatus;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonStep;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

public final class LessonSessionApplicationService {

    private final PrescriptionRepository prescriptionRepository;
    private final EntitlementApplicationService entitlementService;
    private final ResourceCatalogRepository resourceCatalogRepository;
    private final LessonSessionRepository sessionRepository;
    private final LessonSessionTransactionOperations transactions;
    private final LessonSessionKeyGenerator keyGenerator;
    private final Clock clock;

    public LessonSessionApplicationService(
            PrescriptionRepository prescriptionRepository,
            EntitlementApplicationService entitlementService,
            ResourceCatalogRepository resourceCatalogRepository,
            LessonSessionRepository sessionRepository,
            LessonSessionTransactionOperations transactions,
            LessonSessionKeyGenerator keyGenerator,
            Clock clock
    ) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository);
        this.entitlementService = Objects.requireNonNull(entitlementService);
        this.resourceCatalogRepository = Objects.requireNonNull(resourceCatalogRepository);
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.transactions = Objects.requireNonNull(transactions);
        this.keyGenerator = Objects.requireNonNull(keyGenerator);
        this.clock = Objects.requireNonNull(clock);
    }

    public LessonSessionMutationResult start(
            String userKeyValue,
            StartLessonSessionCommand command,
            String idempotencyKey
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        Objects.requireNonNull(command, "command is required");
        String normalizedKey = idempotencyKey(idempotencyKey);
        String requestHash = hash(command);
        return transactions.execute(() -> {
            var replay = sessionRepository.findStartForUpdate(userKey, normalizedKey);
            if (replay.isPresent()) {
                LessonSessionStartRecord record = replay.orElseThrow();
                if (!record.requestHash().equals(requestHash)) {
                    throw new LessonSessionApplicationException(
                            "IDEMPOTENCY_CONFLICT", 409,
                            "Idempotency-Key was already used for another lesson start");
                }
                return new LessonSessionMutationResult(record.session(), true);
            }

            Instant now = clock.instant();
            DailyLearningPrescription prescription = prescriptionRepository
                    .findOwnedForUpdate(userKey, command.prescriptionId())
                    .orElseThrow(LessonSessionApplicationException::stalePrescription);
            if (prescription.status() != PrescriptionStatus.ACTIVE
                    || prescription.version() != command.prescriptionVersion()
                    || !prescription.expiresAt().isAfter(now)) {
                throw LessonSessionApplicationException.stalePrescription();
            }
            PrescriptionBlock block = prescription.blocks().stream()
                    .filter(candidate -> candidate.blockId().equals(command.blockId()))
                    .findFirst()
                    .orElseThrow(LessonSessionApplicationException::stalePrescription);
            if (block.status() != PrescriptionBlockStatus.READY) {
                throw LessonSessionApplicationException.stalePrescription();
            }

            var exactResource = resourceCatalogRepository.findExactVersion(
                    block.resource().resourceKey(), block.resource().resourceVersion());
            if (exactResource.isEmpty()
                    || exactResource.orElseThrow().resourceVersion().status() != ResourceVersionStatus.PUBLISHED) {
                throw LessonSessionApplicationException.stalePrescription();
            }

            AccessDecision decision = entitlementService.decideAuthoritatively(
                    userKey, false, block.resource().resourceKey());
            if (!decision.allowed()) {
                throw denied(decision.reason());
            }

            LessonSession session = LessonSession.start(
                    keyGenerator.nextKey(), prescription.prescriptionId(), Math.toIntExact(prescription.version()),
                    block.blockId(), block.resource().resourceKey(), block.resource().resourceVersion(),
                    block.skillUnitVariantKey(), block.episodeMappingKey(), block.trainingType(),
                    command.inputMode(), now);
            sessionRepository.insert(userKey, session, normalizedKey, requestHash);
            return new LessonSessionMutationResult(session, false);
        });
    }

    public LessonSession get(String userKeyValue, String sessionId) {
        return sessionRepository.findById(new UserKey(userKeyValue), required(sessionId, "sessionId"))
                .orElseThrow(LessonSessionApplicationException::notFound);
    }

    public LessonSession pause(String userKeyValue, String sessionId, String idempotencyKey) {
        idempotencyKey(idempotencyKey);
        return mutate(userKeyValue, sessionId, session -> session.pause(clock.instant()));
    }

    public LessonSession resume(String userKeyValue, String sessionId, String idempotencyKey) {
        idempotencyKey(idempotencyKey);
        return mutate(userKeyValue, sessionId, LessonSession::resume);
    }

    public LessonSession completeStep(
            String userKeyValue,
            String sessionId,
            String stepId,
            String idempotencyKey
    ) {
        idempotencyKey(idempotencyKey);
        LessonStep step;
        try {
            step = LessonStep.valueOf(required(stepId, "stepId"));
        } catch (IllegalArgumentException exception) {
            throw LessonSessionApplicationException.stateConflict("lesson step is not valid");
        }
        LessonStep finalStep = step;
        return mutate(userKeyValue, sessionId, session -> session.completeDeterministicStep(finalStep));
    }

    private LessonSession mutate(
            String userKeyValue,
            String sessionId,
            java.util.function.UnaryOperator<LessonSession> mutation
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        String normalizedSessionId = required(sessionId, "sessionId");
        return transactions.execute(() -> {
            LessonSession current = sessionRepository.findById(userKey, normalizedSessionId)
                    .orElseThrow(LessonSessionApplicationException::notFound);
            LessonSession updated;
            try {
                updated = mutation.apply(current);
            } catch (IllegalStateException exception) {
                throw LessonSessionApplicationException.stateConflict(exception.getMessage());
            }
            if (updated == current) {
                return current;
            }
            return sessionRepository.save(userKey, current.version(), updated);
        });
    }

    private static LessonSessionApplicationException denied(AccessDecisionReason reason) {
        String code = switch (reason) {
            case ENTITLEMENT_REVOKED, ENTITLEMENT_EXPIRED -> "ENTITLEMENT_REVOKED";
            case ENTITLEMENT_REQUIRED, ENTITLEMENT_OWNERSHIP_MISMATCH -> "ENTITLEMENT_REQUIRED";
            default -> "ACCESS_DENIED";
        };
        return new LessonSessionApplicationException(code, 403, "resource access is not available");
    }

    private static String hash(StartLessonSessionCommand command) {
        String canonical = command.prescriptionId().strip() + "|" + command.prescriptionVersion()
                + "|" + command.blockId().strip() + "|" + command.inputMode().name();
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    private static String idempotencyKey(String value) {
        String normalized = required(value, "Idempotency-Key");
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        }
        return normalized;
    }
}
