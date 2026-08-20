package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.entitlement.EntitlementApplicationService;
import cn.forever24.tutor.application.planning.PrescriptionRepository;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessDecisionReason;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.LearnerInputSnapshot;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.planning.PrescriptionBlockStatus;
import cn.forever24.tutor.planning.PrescriptionGoal;
import cn.forever24.tutor.planning.PrescriptionResourceRef;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.planning.PrescriptionTaskHero;
import cn.forever24.tutor.planning.policy.PedagogicalPolicyVersion;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceVersion;
import cn.forever24.tutor.resource.ResourceVersionStatus;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonSessionStatus;
import cn.forever24.tutor.training.LessonStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LessonSessionApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
    private static final UserKey USER = new UserKey("usr-1");

    @Test
    void startIsIdempotentAndLocksPrescriptionResourceVersion() {
        Fixture fixture = fixture(activePrescription(), allowed());
        StartLessonSessionCommand command = command();

        LessonSessionMutationResult first = fixture.service.start(USER.value(), command, "start-1");
        LessonSessionMutationResult replay = fixture.service.start(USER.value(), command, "start-1");

        assertEquals(first.session().sessionId(), replay.session().sessionId());
        assertEquals("1.0.0", replay.session().resourceVersion());
        assertTrue(replay.replayed());
        assertThrows(LessonSessionApplicationException.class, () -> fixture.service.start(
                USER.value(), new StartLessonSessionCommand("prx-1", 1, "other", null), "start-1"));
    }

    @Test
    void rechecksRevokedEntitlementImmediatelyBeforeStart() {
        Fixture fixture = fixture(activePrescription(), AccessDecision.deny(
                AccessDecisionReason.ENTITLEMENT_REVOKED, "premium", NOW));

        LessonSessionApplicationException exception = assertThrows(
                LessonSessionApplicationException.class,
                () -> fixture.service.start(USER.value(), command(), "start-1"));

        assertEquals("ENTITLEMENT_REVOKED", exception.code());
        assertTrue(fixture.repository.sessions.isEmpty());
    }

    @Test
    void rejectsStalePrescriptionVersionBeforeCreatingSession() {
        Fixture fixture = fixture(activePrescription(), allowed());

        LessonSessionApplicationException exception = assertThrows(
                LessonSessionApplicationException.class,
                () -> fixture.service.start(USER.value(),
                        new StartLessonSessionCommand("prx-1", 2, "blk-1", null), "start-1"));

        assertEquals("PRESCRIPTION_STALE", exception.code());
    }

    @Test
    void rejectsPrescribedResourceVersionThatIsNoLongerPublished() {
        Fixture fixture = fixture(activePrescription(), allowed(), ResourceVersionStatus.UNPUBLISHED);

        LessonSessionApplicationException exception = assertThrows(
                LessonSessionApplicationException.class,
                () -> fixture.service.start(USER.value(), command(), "start-1"));

        assertEquals("PRESCRIPTION_STALE", exception.code());
        assertTrue(fixture.repository.sessions.isEmpty());
    }

    @Test
    void ownerIsolationResumeAndInvalidStepTransitionUsePersistedState() {
        Fixture fixture = fixture(activePrescription(), allowed());
        LessonSession started = fixture.service.start(USER.value(), command(), "start-1").session();
        LessonSession firstListen = fixture.service.completeStep(
                USER.value(), started.sessionId(), "SCENE_CONTEXT", "step-1");
        fixture.service.pause(USER.value(), started.sessionId(), "pause-1");
        LessonSession resumed = fixture.service.resume(USER.value(), started.sessionId(), "resume-1");

        assertEquals(LessonSessionStatus.IN_PROGRESS, resumed.status());
        assertEquals(LessonStep.FIRST_LISTEN, fixture.service.get(USER.value(), started.sessionId()).currentStep());
        assertEquals(LessonStep.FIRST_LISTEN, firstListen.currentStep());
        LessonSessionApplicationException notFound = assertThrows(
                LessonSessionApplicationException.class,
                () -> fixture.service.get("other-user", started.sessionId()));
        assertEquals("SESSION_NOT_FOUND", notFound.code());
        LessonSessionApplicationException conflict = assertThrows(
                LessonSessionApplicationException.class,
                () -> fixture.service.completeStep(
                        USER.value(), started.sessionId(), "TRANSCRIPT_EXPRESSIONS", "step-2"));
        assertEquals("SESSION_STATE_CONFLICT", conflict.code());
    }

    private static Fixture fixture(DailyLearningPrescription prescription, AccessDecision decision) {
        return fixture(prescription, decision, ResourceVersionStatus.PUBLISHED);
    }

    private static Fixture fixture(
            DailyLearningPrescription prescription,
            AccessDecision decision,
            ResourceVersionStatus versionStatus
    ) {
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        when(prescriptions.findOwnedForUpdate(USER, prescription.prescriptionId()))
                .thenReturn(Optional.of(prescription));
        EntitlementApplicationService entitlements = mock(EntitlementApplicationService.class);
        when(entitlements.decideAuthoritatively(USER, false, "resource-1")).thenReturn(decision);
        ResourceCatalogRepository catalog = mock(ResourceCatalogRepository.class);
        ResourceCatalogEntry entry = mock(ResourceCatalogEntry.class);
        ResourceVersion version = mock(ResourceVersion.class);
        when(entry.resourceVersion()).thenReturn(version);
        when(version.status()).thenReturn(versionStatus);
        when(catalog.findExactVersion("resource-1", "1.0.0")).thenReturn(Optional.of(entry));
        FakeLessonSessionRepository repository = new FakeLessonSessionRepository();
        LessonSessionTransactionOperations direct = new LessonSessionTransactionOperations() {
            @Override
            public <T> T execute(Supplier<T> action) {
                return action.get();
            }
        };
        LessonSessionApplicationService service = new LessonSessionApplicationService(
                prescriptions, entitlements, catalog, repository, direct, () -> "lsn-1",
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, repository);
    }

    private static StartLessonSessionCommand command() {
        return new StartLessonSessionCommand("prx-1", 1, "blk-1", LessonInputMode.VOICE_OR_TEXT);
    }

    private static AccessDecision allowed() {
        return AccessDecision.allow(AccessDecisionReason.ALLOWED_PUBLIC, "public", NOW);
    }

    private static DailyLearningPrescription activePrescription() {
        PrescriptionBlock block = new PrescriptionBlock(
                "blk-1", 1, BlockType.OUTPUT, "Airport gate confirmation", "skill.a2",
                new PrescriptionResourceRef("resource-1", "1.0.0"), "mapping-1",
                "S01", "EP006", "GATE_CHANGE", CefrLevel.A2, ScaffoldingLevel.HIGH,
                TrainingType.ROLE_PLAY, 5, List.of("confirm_information"),
                new CompletionPolicy(1, Set.of("confirm_information"), true), null,
                Map.of("SKILL_GAP", BigDecimal.ONE),
                new PrescriptionTaskHero("hero-1", "https://cdn.invalid/hero.webp", "16:9",
                        new BigDecimal("0.5"), new BigDecimal("0.5"),
                        "Lin Muen stands at the airport gate."),
                PrescriptionBlockStatus.READY);
        return new DailyLearningPrescription(
                "prx-1", USER, LocalDate.of(2026, 8, 20), ZoneId.of("Asia/Shanghai"),
                1, PrescriptionStatus.ACTIVE,
                new PrescriptionGoal("TRAVEL", "Travel"), List.of(block), "Practice airport confirmation.",
                List.of("GOAL_MATCH"), PedagogicalPolicyVersion.V2_P0_1,
                new LearnerInputSnapshot(1, 10, "TRAVEL", null, List.of(
                        new PrescriptionSkillState(
                                "speaking", new BigDecimal("0.4"), new BigDecimal("0.7"),
                                CefrLevel.A2, 1, NOW.minusSeconds(3600)))),
                NOW.minusSeconds(60), NOW.plusSeconds(3600), null);
    }

    private record Fixture(LessonSessionApplicationService service, FakeLessonSessionRepository repository) {
    }

    private static final class FakeLessonSessionRepository implements LessonSessionRepository {
        private final Map<String, LessonSession> sessions = new HashMap<>();
        private final Map<String, LessonSessionStartRecord> starts = new HashMap<>();

        @Override
        public Optional<LessonSessionStartRecord> findStartForUpdate(UserKey userKey, String idempotencyKey) {
            return Optional.ofNullable(starts.get(userKey.value() + idempotencyKey));
        }

        @Override
        public void insert(UserKey userKey, LessonSession session, String idempotencyKey, String requestHash) {
            sessions.put(userKey.value() + session.sessionId(), session);
            starts.put(userKey.value() + idempotencyKey, new LessonSessionStartRecord(requestHash, session));
        }

        @Override
        public Optional<LessonSession> findById(UserKey userKey, String sessionId) {
            return Optional.ofNullable(sessions.get(userKey.value() + sessionId));
        }

        @Override
        public LessonSession save(UserKey userKey, long expectedVersion, LessonSession session) {
            LessonSession current = findById(userKey, session.sessionId()).orElseThrow();
            if (current.version() != expectedVersion) {
                throw LessonSessionApplicationException.versionConflict();
            }
            sessions.put(userKey.value() + session.sessionId(), session);
            starts.replaceAll((key, record) -> record.session().sessionId().equals(session.sessionId())
                    ? new LessonSessionStartRecord(record.requestHash(), session) : record);
            return session;
        }
    }
}
