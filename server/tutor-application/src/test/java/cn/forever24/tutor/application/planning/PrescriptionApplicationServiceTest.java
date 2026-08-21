package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.application.curriculum.CurriculumRepository;
import cn.forever24.tutor.application.experience.ExperienceRepository;
import cn.forever24.tutor.application.resource.MediaAccessGrant;
import cn.forever24.tutor.application.resource.MediaAccessUrlIssuer;
import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.CurriculumStatus;
import cn.forever24.tutor.curriculum.DurationRange;
import cn.forever24.tutor.curriculum.EvidenceCriterion;
import cn.forever24.tutor.curriculum.MasteryImpactPolicy;
import cn.forever24.tutor.curriculum.RetryPolicy;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.SkillUnitVariant;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.experience.Episode;
import cn.forever24.tutor.experience.EpisodeMapping;
import cn.forever24.tutor.experience.ExperienceCatalog;
import cn.forever24.tutor.experience.ExperienceFitInputs;
import cn.forever24.tutor.experience.ExperienceStatus;
import cn.forever24.tutor.experience.MappingResourceReference;
import cn.forever24.tutor.experience.Scene;
import cn.forever24.tutor.experience.Season;
import cn.forever24.tutor.experience.StoryTransition;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.AssetGenerationMetadata;
import cn.forever24.tutor.resource.AssetMediaType;
import cn.forever24.tutor.resource.AssetPurpose;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.DisplaySurface;
import cn.forever24.tutor.resource.FocalPoint;
import cn.forever24.tutor.resource.ImageAssetMetadata;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceType;
import cn.forever24.tutor.resource.ShotType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrescriptionApplicationServiceTest {

    private static final UserKey USER = new UserKey("usr-prescription");
    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);

    @Test
    void differentSkillStatesProduceDifferentGapFactors() {
        DailyLearningPrescription low = fixture("0.20", candidates(), passAll()).service()
                .getOrGenerateToday(USER.value(), TODAY, "Asia/Shanghai");
        DailyLearningPrescription medium = fixture("0.60", candidates(), passAll()).service()
                .getOrGenerateToday(USER.value(), TODAY, "Asia/Shanghai");

        assertNotEquals(
                low.blocks().getFirst().recommendationFactors().get("SKILL_GAP"),
                medium.blocks().getFirst().recommendationFactors().get("SKILL_GAP"));
        assertTrue(low.blocks().getFirst().expectedEvidence().contains("confirm_information"));
        assertEquals("1.0.0", low.blocks().getFirst().resource().resourceVersion());
    }

    @Test
    void regenerationIsIdempotentAndSupersedesThePreviousVersion() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription current = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);
        RegeneratePrescriptionCommand command = new RegeneratePrescriptionCommand(
                current.prescriptionId(), current.version(), PrescriptionFeedbackReason.TIME_INSUFFICIENT,
                5, null, "only five minutes");

        PrescriptionMutationResult first = fixture.service().regenerate(USER.value(), command, "regen-key");
        PrescriptionMutationResult replay = fixture.service().regenerate(USER.value(), command, "regen-key");

        assertFalse(first.replayed());
        assertTrue(replay.replayed());
        assertEquals(first.prescription().prescriptionId(), replay.prescription().prescriptionId());
        assertEquals(2, first.prescription().version());
        assertEquals(5, first.prescription().inputSnapshot().availableMinutes());
        assertEquals(PrescriptionStatus.SUPERSEDED,
                fixture.repository().findOwned(USER, current.prescriptionId()).orElseThrow().status());
    }

    @Test
    void temporaryGoalIsAppliedOnlyToTheReplacementSnapshot() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription current = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);
        PrescriptionMutationResult result = fixture.service().regenerate(
                USER.value(),
                new RegeneratePrescriptionCommand(
                        current.prescriptionId(), current.version(), PrescriptionFeedbackReason.TEMPORARY_GOAL,
                        null, "airport interview", null),
                "temporary-goal-key");

        assertEquals("TEMPORARY_GOAL", result.prescription().priorityGoal().code());
        assertEquals("airport interview", result.prescription().inputSnapshot().temporaryGoal());
        assertEquals("GENERAL_COMMUNICATION", current.priorityGoal().code());
    }

    @Test
    void accessFilterRunsBeforeRankingAndNoCandidateIsExplicit() {
        AtomicInteger filteredCount = new AtomicInteger();
        PrescriptionCandidateAccessFilter denyAll = (user, values) -> {
            filteredCount.set(values.size());
            return List.of();
        };
        PrescriptionApplicationException exception = assertThrows(
                PrescriptionApplicationException.class,
                () -> fixture("0.30", candidates(), denyAll).service()
                        .getOrGenerateToday(USER.value(), TODAY, null));

        assertEquals(2, filteredCount.get());
        assertEquals("PRESCRIPTION_NO_CANDIDATE", exception.code());
        assertEquals(Boolean.FALSE, exception.fallbackAvailable());
    }

    @Test
    void anotherLearnerCannotRegenerateAnOwnedPrescription() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription current = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);

        PrescriptionApplicationException exception = assertThrows(
                PrescriptionApplicationException.class,
                () -> fixture.service().regenerate(
                        "usr-other",
                        new RegeneratePrescriptionCommand(
                                current.prescriptionId(), current.version(),
                                PrescriptionFeedbackReason.TIME_INSUFFICIENT, 5, null, null),
                        "other-user-key"));

        assertEquals("PRESCRIPTION_NOT_FOUND", exception.code());
    }

    @Test
    void concurrentInitialGenerationReturnsOnePersistedPrescription() throws Exception {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        Callable<String> request = () -> fixture.service()
                .getOrGenerateToday(USER.value(), TODAY, null).prescriptionId();
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<String>> futures = executor.invokeAll(java.util.Collections.nCopies(24, request));
            Set<String> ids = new java.util.HashSet<>();
            for (Future<String> future : futures) {
                ids.add(future.get());
            }
            assertEquals(1, ids.size());
        }
    }

    @Test
    void skipRecordsTheBlockWithoutRemovingTheLastOutput() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription generated = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);
        PrescriptionBlock first = generated.blocks().getFirst();
        PrescriptionBlock second = new PrescriptionBlock(
                "block-second", 2, first.type(), first.title(), first.skillUnitVariantKey(),
                first.resource(), first.episodeMappingKey(), first.seasonKey(), first.episodeKey(),
                first.sceneKey(), first.difficulty(), first.scaffolding(), first.trainingType(),
                first.estimatedMinutes(), first.expectedEvidence(), first.completionPolicy(), first.fallback(),
                first.recommendationFactors(), first.taskHero(), first.status());
        DailyLearningPrescription withTwoOutputs = new DailyLearningPrescription(
                generated.prescriptionId(), generated.userKey(), generated.learningDate(), generated.learnerZone(),
                generated.version(), generated.status(), generated.priorityGoal(), List.of(first, second),
                generated.rationale(), generated.reasonCodes(), generated.policyVersion(),
                new cn.forever24.tutor.planning.LearnerInputSnapshot(
                        generated.inputSnapshot().profileVersion(), 20,
                        generated.inputSnapshot().primaryGoal(), generated.inputSnapshot().temporaryGoal(),
                        generated.inputSnapshot().skillStates()),
                generated.generatedAt(), generated.expiresAt(), generated.supersedesPrescriptionId());
        fixture.repository().seed(withTwoOutputs);

        PrescriptionMutationResult result = fixture.service().skipBlock(
                USER.value(), generated.prescriptionId(), first.blockId(), "already familiar", null, "skip-key");

        assertEquals(cn.forever24.tutor.planning.PrescriptionBlockStatus.SKIPPED,
                result.prescription().blocks().getFirst().status());
        assertEquals(cn.forever24.tutor.planning.PrescriptionBlockStatus.READY,
                result.prescription().blocks().get(1).status());
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayloadAndAStaleVersion() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription current = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);
        RegeneratePrescriptionCommand command = new RegeneratePrescriptionCommand(
                current.prescriptionId(), current.version(), PrescriptionFeedbackReason.TIME_INSUFFICIENT,
                5, null, null);
        fixture.service().regenerate(USER.value(), command, "same-key");

        PrescriptionApplicationException conflict = assertThrows(
                PrescriptionApplicationException.class,
                () -> fixture.service().regenerate(USER.value(), new RegeneratePrescriptionCommand(
                        current.prescriptionId(), current.version(), PrescriptionFeedbackReason.TIME_INSUFFICIENT,
                        4, null, null), "same-key"));
        assertEquals("IDEMPOTENCY_CONFLICT", conflict.code());

        PrescriptionApplicationException stale = assertThrows(
                PrescriptionApplicationException.class,
                () -> fixture.service().regenerate(USER.value(), command, "another-key"));
        assertEquals("PRESCRIPTION_STALE", stale.code());
    }

    @Test
    void learnerMemorySignalRecomposesActivePrescriptionWithExplainableErrorAndReviewFactors() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription initial = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);
        fixture.learner().setMemory(new LearnerMemory(
                List.of(new LearnerMemory.WeakPoint("missing_confirmation", "speaking", 2, "HIGH", NOW)),
                List.of(),
                List.of(new LearnerMemory.DueReview("SKILL", "speaking", NOW.minusSeconds(60), new BigDecimal("0.9000")))));

        DailyLearningPrescription recomposed = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);

        assertEquals(initial.version() + 1, recomposed.version());
        assertEquals(new BigDecimal("1.0000"), recomposed.blocks().getFirst().recommendationFactors().get("ERROR_MATCH"));
        assertTrue(recomposed.reasonCodes().contains("REVIEW_DUE"));
        assertTrue(recomposed.reasonCodes().contains("LEARNING_SIGNAL_RECOMPOSED"));
    }

    @Test
    void highFrequencyCriticalMemoryReducesScaffoldingForTheNextAttempt() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        fixture.learner().setMemory(new LearnerMemory(
                List.of(new LearnerMemory.WeakPoint("missing_confirmation", "speaking", 2, "HIGH", NOW)),
                List.of(), List.of()));

        DailyLearningPrescription prescription = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);

        assertEquals(ScaffoldingLevel.HIGH, prescription.blocks().getFirst().scaffolding());
    }

    @Test
    void topicRejectionRejectsEveryAlreadySelectedTopicWhenNoAlternativeRemains() {
        Fixture fixture = fixture("0.30", candidates(), passAll());
        DailyLearningPrescription initial = fixture.service().getOrGenerateToday(USER.value(), TODAY, null);

        PrescriptionApplicationException exception = assertThrows(PrescriptionApplicationException.class, () -> fixture.service().regenerate(USER.value(), new RegeneratePrescriptionCommand(
                initial.prescriptionId(), initial.version(), PrescriptionFeedbackReason.TOPIC_REJECTED,
                null, null, "another topic"), "reject-topic"));
        assertEquals("PRESCRIPTION_NO_CANDIDATE", exception.code());
    }

    private static Fixture fixture(
            String mastery,
            List<PublishedResourceCandidate> resources,
            PrescriptionCandidateAccessFilter accessFilter
    ) {
        CurriculumRepository curriculum = mock(CurriculumRepository.class);
        ResourceCatalogRepository resource = mock(ResourceCatalogRepository.class);
        ExperienceRepository experience = mock(ExperienceRepository.class);
        when(curriculum.findVariants(any())).thenReturn(List.of(variant()));
        when(resource.findPublishedCandidates(any())).thenReturn(resources);
        when(experience.findCatalog()).thenReturn(Optional.of(experience()));
        TestPrescriptionRepository repository = new TestPrescriptionRepository();
        MutableLearnerSnapshotLoader learner = new MutableLearnerSnapshotLoader(mastery);
        AtomicInteger keys = new AtomicInteger();
        PrescriptionKeyGenerator keyGenerator = new PrescriptionKeyGenerator() {
            @Override
            public String nextPrescriptionKey() {
                return "prescription-" + keys.incrementAndGet();
            }

            @Override
            public String nextBlockKey() {
                return "block-" + keys.incrementAndGet();
            }
        };
        MediaAccessUrlIssuer media = new MediaAccessUrlIssuer() {
            @Override
            public MediaAccessGrant publicUrl(ResourceAsset asset) {
                return new MediaAccessGrant(URI.create("https://cdn.example.invalid/" + asset.assetKey()), null);
            }

            @Override
            public MediaAccessGrant issuePrivate(
                    UserKey userKey,
                    String resourceKey,
                    ResourceAsset asset,
                    String idempotencyKey,
                    Instant expiresAt
            ) {
                throw new AssertionError("private URL must not be issued while composing a prescription");
            }
        };
        PrescriptionApplicationService service = new PrescriptionApplicationService(
                learner, curriculum, resource, accessFilter, experience, repository, media, keyGenerator,
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, repository, learner);
    }

    private static PrescriptionCandidateAccessFilter passAll() {
        return (user, values) -> values;
    }

    private static SkillUnitVariant variant() {
        EvidenceCriterion evidence = new EvidenceCriterion(
                "confirm_information", "Confirms changed information accurately",
                BigDecimal.ONE, true, 0);
        return new SkillUnitVariant(
                "travel.confirm-information.a2",
                CefrLevel.A2,
                2,
                new DurationRange(4, 5),
                Set.of(TrainingType.ROLE_PLAY),
                Set.of(ScaffoldingLevel.HIGH, ScaffoldingLevel.MEDIUM),
                Set.of("missing_confirmation"),
                Set.of("speaking"),
                Set.of(),
                Set.of(),
                List.of(evidence),
                new CompletionPolicy(1, Set.of(evidence.criterionKey()), true),
                new RetryPolicy(true, 2, true),
                new MasteryImpactPolicy(true, false),
                CurriculumStatus.ACTIVE);
    }

    private static List<PublishedResourceCandidate> candidates() {
        ResourceAsset hero = hero();
        return List.of(new PublishedResourceCandidate(
                "season1.ep006.gate-change.a2", "1.0.0", "internal", "public-season1",
                ResourceType.SCENARIO_LESSON, "Airport gate confirmation", CefrLevel.A2,
                "airport", "GATE_CHANGE", "Confirm changed boarding information",
                AccessScope.PUBLIC, 5, Set.of("travel.confirm-information.a2"), hero, List.of(hero)),
                new PublishedResourceCandidate(
                        "season1.ep009.hotel-confirmation.a2", "1.0.0", "internal", "public-season1",
                        ResourceType.SCENARIO_LESSON, "Hotel booking confirmation", CefrLevel.A2,
                        "hotel", "BOOKING", "Confirm a hotel booking",
                        AccessScope.PUBLIC, 5, Set.of("travel.confirm-information.a2"), hero, List.of(hero)));
    }

    private static ResourceAsset hero() {
        ImageAssetMetadata metadata = new ImageAssetMetadata(
                "Lin Muen stands full body beside the airport boarding gate display.",
                new AssetGenerationMetadata("test", "image-model", "2026-08", "1.0.0"),
                Set.of("lin-muen-character-reference"),
                "16:9",
                ShotType.ENVIRONMENTAL_FULL_BODY,
                Set.of(DisplaySurface.PRESCRIPTION_CARD, DisplaySurface.SCENARIO_INTRO, DisplaySurface.SCENARIO_TRAINING),
                new FocalPoint(0.68, 0.42),
                "Lin Muen stands near an airport gate and checks the changed boarding information.",
                "GATE_CHANGE",
                "season1-airport");
        return new ResourceAsset(
                "asset-task-hero-airport", "1.0.0", AssetMediaType.IMAGE, AssetPurpose.TASK_HERO,
                "images/season1/airport-task-hero.webp", "sha256:" + "a".repeat(64),
                "image/webp", 1024, AccessScope.PUBLIC, metadata, AssetStatus.ACTIVE, NOW);
    }

    private static ExperienceCatalog experience() {
        Season season = new Season("S01", "Getting Closer to English", ExperienceStatus.ACTIVE,
                "{\"character\":\"Lin Muen\"}");
        Episode episode = new Episode(
                "EP006", "S01", "Airport Adventure", "Lin Muen asks for help at the gate.",
                false, ExperienceStatus.ACTIVE, "{}", 6);
        Scene scene = new Scene(
                "GATE_CHANGE", "EP006", "Gate Change", "Airport gate",
                "Lin Muen visibly participates in the airport gate task.",
                "{\"character\":\"Lin Muen\"}", ExperienceStatus.ACTIVE);
        EpisodeMapping mapping = new EpisodeMapping(
                "s01.ep006.gate-change.a2", "travel.confirm-information.a2",
                "S01", "EP006", "GATE_CHANGE", Set.of(CefrLevel.A2),
                new StoryTransition("Lin Muen notices the gate change.", "Lin Muen reaches the gate.", false),
                new ExperienceFitInputs(Set.of("general"), Set.of("airport"), Set.of("clarification"), Set.of()),
                null,
                ExperienceStatus.ACTIVE,
                List.of(new MappingResourceReference("season1.ep006.gate-change.a2", "1.0.0", 0),
                        new MappingResourceReference("season1.ep009.hotel-confirmation.a2", "1.0.0", 1)));
        return new ExperienceCatalog(List.of(season), List.of(episode), List.of(scene), List.of(mapping));
    }

    private record Fixture(PrescriptionApplicationService service, TestPrescriptionRepository repository,
                           MutableLearnerSnapshotLoader learner) {
    }

    private static final class MutableLearnerSnapshotLoader implements LearnerSnapshotLoader {
        private final String mastery;
        private volatile LearnerMemory memory = LearnerMemory.empty();

        private MutableLearnerSnapshotLoader(String mastery) {
            this.mastery = mastery;
        }

        void setMemory(LearnerMemory memory) {
            this.memory = memory;
        }

        @Override
        public LearnerPlanningSnapshot load(UserKey ignored) {
            return new LearnerPlanningSnapshot(USER, PrimaryGoal.GENERAL, ZoneId.of("Asia/Shanghai"), 20, 7, CefrLevel.A2,
                    List.of(new PrescriptionSkillState("speaking", new BigDecimal(mastery), new BigDecimal("0.75"),
                            CefrLevel.A2, 2, NOW.minusSeconds(86_400))), memory);
        }
    }

    private static final class TestPrescriptionRepository implements PrescriptionRepository {
        private final Map<String, DailyLearningPrescription> values = new ConcurrentHashMap<>();
        private final Map<String, String> active = new ConcurrentHashMap<>();
        private final Map<String, Replay> replays = new ConcurrentHashMap<>();

        void seed(DailyLearningPrescription prescription) {
            values.put(prescription.prescriptionId(), prescription);
            active.put(prescription.userKey().value() + prescription.learningDate(), prescription.prescriptionId());
        }

        @Override
        public Optional<DailyLearningPrescription> findActive(UserKey userKey, LocalDate learningDate) {
            return Optional.ofNullable(active.get(userKey.value() + learningDate)).map(values::get);
        }

        @Override
        public Optional<DailyLearningPrescription> findOwned(UserKey userKey, String prescriptionId) {
            DailyLearningPrescription value = values.get(prescriptionId);
            return value != null && value.userKey().equals(userKey) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Optional<PrescriptionMutationResult> findReplay(
                UserKey userKey,
                String operation,
                String idempotencyKey,
                String requestHash
        ) {
            Replay replay = replays.get(userKey.value() + operation + idempotencyKey);
            if (replay == null) {
                return Optional.empty();
            }
            if (!replay.requestHash().equals(requestHash)) {
                throw new PrescriptionApplicationException("IDEMPOTENCY_CONFLICT", 409, "idempotency conflict");
            }
            return Optional.of(new PrescriptionMutationResult(values.get(replay.prescriptionId()), true));
        }

        @Override
        public synchronized DailyLearningPrescription saveInitialIfAbsent(DailyLearningPrescription prescription) {
            String key = prescription.userKey().value() + prescription.learningDate();
            String existing = active.get(key);
            if (existing != null) {
                return values.get(existing);
            }
            values.put(prescription.prescriptionId(), prescription);
            active.put(key, prescription.prescriptionId());
            return prescription;
        }

        @Override
        public synchronized PrescriptionMutationResult replaceActive(
                DailyLearningPrescription expectedCurrent,
                DailyLearningPrescription replacement,
                PrescriptionFeedback feedback,
                String idempotencyKey,
                String requestHash
        ) {
            String replayKey = expectedCurrent.userKey().value() + "REGENERATE" + idempotencyKey;
            Replay replay = replays.get(replayKey);
            if (replay != null) {
                if (!replay.requestHash().equals(requestHash)) {
                    throw new PrescriptionApplicationException(
                            "IDEMPOTENCY_CONFLICT", 409, "idempotency conflict");
                }
                return new PrescriptionMutationResult(values.get(replay.prescriptionId()), true);
            }
            String key = expectedCurrent.userKey().value() + expectedCurrent.learningDate();
            if (!expectedCurrent.prescriptionId().equals(active.get(key))) {
                throw new PrescriptionApplicationException("PRESCRIPTION_STALE", 409, "stale");
            }
            values.put(expectedCurrent.prescriptionId(), expectedCurrent.superseded());
            values.put(replacement.prescriptionId(), replacement);
            active.put(key, replacement.prescriptionId());
            replays.put(replayKey, new Replay(requestHash, replacement.prescriptionId()));
            return new PrescriptionMutationResult(replacement, false);
        }

        @Override
        public PrescriptionMutationResult saveBlockSkip(
                DailyLearningPrescription expectedCurrent,
                DailyLearningPrescription updated,
                PrescriptionFeedback feedback,
                String idempotencyKey,
                String requestHash
        ) {
            values.put(updated.prescriptionId(), updated);
            return new PrescriptionMutationResult(updated, false);
        }

        private record Replay(String requestHash, String prescriptionId) {
        }
    }
}
