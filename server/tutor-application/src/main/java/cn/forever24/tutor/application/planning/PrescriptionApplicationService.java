package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.application.curriculum.CurriculumRepository;
import cn.forever24.tutor.application.curriculum.CurriculumVariantQuery;
import cn.forever24.tutor.application.experience.ExperienceRepository;
import cn.forever24.tutor.application.resource.MediaAccessUrlIssuer;
import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.application.resource.ResourceCandidateQuery;
import cn.forever24.tutor.application.resource.ResourceCatalogRepository;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CurriculumStatus;
import cn.forever24.tutor.curriculum.EvidenceCriterion;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.SkillUnitVariant;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.experience.EpisodeMapping;
import cn.forever24.tutor.experience.EpisodeMappingResolver;
import cn.forever24.tutor.experience.ExperienceCatalog;
import cn.forever24.tutor.experience.ExperienceResolution;
import cn.forever24.tutor.experience.ExperienceResolutionRequest;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.LearnerInputSnapshot;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.planning.PrescriptionBlockStatus;
import cn.forever24.tutor.planning.PrescriptionGoal;
import cn.forever24.tutor.planning.PrescriptionResourceRef;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.planning.PrescriptionTaskHero;
import cn.forever24.tutor.planning.policy.DifficultyPolicy;
import cn.forever24.tutor.planning.policy.InterleavingPolicy;
import cn.forever24.tutor.planning.policy.MasteryEligibilityPolicy;
import cn.forever24.tutor.planning.policy.PedagogicalPolicyVersion;
import cn.forever24.tutor.planning.policy.PrerequisitePolicy;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.ImageAssetMetadata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PrescriptionApplicationService {

    private final LearnerSnapshotLoader learnerSnapshotLoader;
    private final CurriculumRepository curriculumRepository;
    private final ResourceCatalogRepository resourceRepository;
    private final PrescriptionCandidateAccessFilter accessFilter;
    private final ExperienceRepository experienceRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MediaAccessUrlIssuer mediaAccessUrlIssuer;
    private final PrescriptionKeyGenerator keyGenerator;
    private final Clock clock;
    private final PrescriptionRankingPolicy rankingPolicy = new PrescriptionRankingPolicy();
    private final InterleavingPolicy interleavingPolicy = new InterleavingPolicy();
    private final PrerequisitePolicy prerequisitePolicy = new PrerequisitePolicy();
    private final MasteryEligibilityPolicy masteryEligibilityPolicy = new MasteryEligibilityPolicy();
    private final DifficultyPolicy difficultyPolicy = new DifficultyPolicy();
    private final EpisodeMappingResolver mappingResolver = new EpisodeMappingResolver();

    public PrescriptionApplicationService(
            LearnerSnapshotLoader learnerSnapshotLoader,
            CurriculumRepository curriculumRepository,
            ResourceCatalogRepository resourceRepository,
            PrescriptionCandidateAccessFilter accessFilter,
            ExperienceRepository experienceRepository,
            PrescriptionRepository prescriptionRepository,
            MediaAccessUrlIssuer mediaAccessUrlIssuer,
            PrescriptionKeyGenerator keyGenerator,
            Clock clock
    ) {
        this.learnerSnapshotLoader = java.util.Objects.requireNonNull(learnerSnapshotLoader);
        this.curriculumRepository = java.util.Objects.requireNonNull(curriculumRepository);
        this.resourceRepository = java.util.Objects.requireNonNull(resourceRepository);
        this.accessFilter = java.util.Objects.requireNonNull(accessFilter);
        this.experienceRepository = java.util.Objects.requireNonNull(experienceRepository);
        this.prescriptionRepository = java.util.Objects.requireNonNull(prescriptionRepository);
        this.mediaAccessUrlIssuer = java.util.Objects.requireNonNull(mediaAccessUrlIssuer);
        this.keyGenerator = java.util.Objects.requireNonNull(keyGenerator);
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    public DailyLearningPrescription getOrGenerateToday(
            String userKeyValue,
            LocalDate requestedDate,
            String requestedTimezone
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        LearnerPlanningSnapshot learner = learnerSnapshotLoader.load(userKey);
        LocalDate today = LocalDate.now(clock.withZone(learner.timezone()));
        LocalDate learningDate = requestedDate == null ? today : requestedDate;
        if (!learningDate.equals(today)) {
            throw new PrescriptionApplicationException(
                    "PRESCRIPTION_DATE_NOT_TODAY", 400, "P0 only supports the learner's current local date");
        }
        validateTimezone(requestedTimezone, learner.timezone());
        DailyLearningPrescription active = prescriptionRepository.findActive(userKey, learningDate)
                .filter(existing -> existing.expiresAt().isAfter(clock.instant()))
                .orElse(null);
        if (active == null) {
            return prescriptionRepository.saveInitialIfAbsent(generate(
                    learner, learningDate, 1, null, null, learner.dailyMinutes()));
        }
        String signal = learningSignal(learner);
        if (signal.equals(active.inputSnapshot().learningSignal())) {
            return active;
        }
        PrescriptionFeedback signalFeedback = new PrescriptionFeedback(
                PrescriptionFeedbackReason.LEARNING_SIGNAL, null, null, null, null);
        DailyLearningPrescription replacement = generate(
                learner, learningDate, active.version() + 1, active, signalFeedback,
                active.inputSnapshot().availableMinutes());
        return prescriptionRepository.replaceActive(active, replacement, signalFeedback,
                "signal-" + signal, requestHash("LEARNING_SIGNAL", active.prescriptionId() + "|" + signal)).prescription();
    }

    public DailyLearningPrescription getPrescription(String userKeyValue, String prescriptionId) {
        UserKey userKey = new UserKey(userKeyValue);
        return prescriptionRepository.findOwned(userKey, required(prescriptionId, "prescriptionId"))
                .orElseThrow(() -> new PrescriptionApplicationException(
                        "PRESCRIPTION_NOT_FOUND", 404, "prescription was not found"));
    }

    public PrescriptionMutationResult regenerate(
            String userKeyValue,
            RegeneratePrescriptionCommand command,
            String idempotencyKey
    ) {
        if (command == null) {
            throw new PrescriptionApplicationException(
                    "INVALID_PRESCRIPTION_FEEDBACK", 400, "regeneration command is required");
        }
        String normalizedKey = idempotencyKey(idempotencyKey);
        String requestHash = requestHash("REGENERATE", command.toString());
        UserKey userKey = new UserKey(userKeyValue);
        PrescriptionMutationResult replay = prescriptionRepository.findReplay(
                userKey, "REGENERATE", normalizedKey, requestHash).orElse(null);
        if (replay != null) {
            return replay;
        }
        PrescriptionFeedback feedback;
        try {
            feedback = command.feedback();
        } catch (IllegalArgumentException exception) {
            throw new PrescriptionApplicationException(
                    "INVALID_PRESCRIPTION_FEEDBACK", 400, exception.getMessage());
        }
        DailyLearningPrescription current = prescriptionRepository.findOwned(userKey, command.currentPrescriptionId())
                .orElseThrow(() -> new PrescriptionApplicationException(
                        "PRESCRIPTION_NOT_FOUND", 404, "prescription was not found"));
        DailyLearningPrescription active = prescriptionRepository.findActive(userKey, current.learningDate())
                .orElseThrow(() -> new PrescriptionApplicationException(
                        "PRESCRIPTION_STALE", 409, "prescription is no longer active"));
        if (!active.prescriptionId().equals(current.prescriptionId())
                || current.version() != command.currentVersion()
                || current.status() != PrescriptionStatus.ACTIVE) {
            throw new PrescriptionApplicationException(
                    "PRESCRIPTION_STALE", 409, "prescription version is stale");
        }

        LearnerPlanningSnapshot learner = learnerSnapshotLoader.load(userKey);
        int availableMinutes = feedback.availableMinutes() == null
                ? current.inputSnapshot().availableMinutes()
                : feedback.availableMinutes();
        DailyLearningPrescription replacement = generate(
                learner,
                current.learningDate(),
                current.version() + 1,
                current,
                feedback,
                availableMinutes);
        return prescriptionRepository.replaceActive(
                current,
                replacement,
                feedback,
                normalizedKey,
                requestHash);
    }

    public PrescriptionMutationResult skipBlock(
            String userKeyValue,
            String prescriptionId,
            String blockId,
            String reason,
            String note,
            String idempotencyKey
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        String normalizedBlock = required(blockId, "blockId");
        String normalizedReason = required(reason, "reason");
        String normalizedIdempotencyKey = idempotencyKey(idempotencyKey);
        String requestHash = requestHash("SKIP", required(prescriptionId, "prescriptionId") + "|"
                + normalizedBlock + "|" + normalizedReason + "|" + optional(note));
        PrescriptionMutationResult replay = prescriptionRepository.findReplay(
                userKey, "SKIP", normalizedIdempotencyKey, requestHash).orElse(null);
        if (replay != null) {
            return replay;
        }
        DailyLearningPrescription current = prescriptionRepository.findOwned(
                        userKey, required(prescriptionId, "prescriptionId"))
                .orElseThrow(() -> new PrescriptionApplicationException(
                        "PRESCRIPTION_NOT_FOUND", 404, "prescription was not found"));
        if (current.status() != PrescriptionStatus.ACTIVE) {
            throw new PrescriptionApplicationException(
                    "PRESCRIPTION_STALE", 409, "prescription is no longer active");
        }
        PrescriptionFeedback feedback = new PrescriptionFeedback(
                PrescriptionFeedbackReason.BLOCK_SKIPPED,
                null,
                null,
                normalizedReason + (optional(note) == null ? "" : ": " + optional(note)),
                normalizedBlock);
        DailyLearningPrescription updated;
        try {
            updated = current.skipBlock(normalizedBlock);
        } catch (IllegalStateException exception) {
            throw new PrescriptionApplicationException(
                    "PRESCRIPTION_OUTPUT_REQUIRED", 409, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw new PrescriptionApplicationException(
                    "PRESCRIPTION_BLOCK_NOT_FOUND", 404, exception.getMessage());
        }
        return prescriptionRepository.saveBlockSkip(
                current,
                updated,
                feedback,
                normalizedIdempotencyKey,
                requestHash);
    }

    private DailyLearningPrescription generate(
            LearnerPlanningSnapshot learner,
            LocalDate learningDate,
            long version,
            DailyLearningPrescription superseded,
            PrescriptionFeedback feedback,
            int availableMinutes
    ) {
        CefrLevel targetLevel = adjustedLevel(learner.currentLevel(), feedback);
        List<SkillUnitVariant> variants = curriculumRepository.findVariants(
                new CurriculumVariantQuery(targetLevel, null, CurriculumStatus.ACTIVE));
        Map<String, SkillUnitVariant> variantsByKey = variants.stream()
                .collect(Collectors.toMap(SkillUnitVariant::variantKey, Function.identity()));
        List<PublishedResourceCandidate> published = resourceRepository.findPublishedCandidates(
                new ResourceCandidateQuery(targetLevel, null, null, null, null));
        List<PublishedResourceCandidate> accessible = accessFilter.accessibleFor(
                learner.userKey(), published);
        if (feedback != null && feedback.reason() == PrescriptionFeedbackReason.TOPIC_REJECTED
                && superseded != null) {
            Set<String> rejectedResources = superseded.blocks().stream()
                    .map(block -> block.resource().resourceKey())
                    .collect(Collectors.toSet());
            accessible = accessible.stream()
                    .filter(candidate -> !rejectedResources.contains(candidate.resourceKey()))
                    .toList();
        }

        Map<String, PrescriptionSkillState> skillStates = learner.skillStates().stream()
                .collect(Collectors.toMap(PrescriptionSkillState::skillKey, Function.identity()));
        Map<String, CandidateContext> contexts = new LinkedHashMap<>();
        List<PrescriptionRankingPolicy.Candidate> rankingCandidates = new ArrayList<>();
        for (PublishedResourceCandidate resource : accessible) {
            for (String variantKey : resource.skillUnitVariantKeys().stream().sorted().toList()) {
                SkillUnitVariant variant = variantsByKey.get(variantKey);
                if (variant == null || !eligible(variant, skillStates)) {
                    continue;
                }
                BlockType blockType = blockType(variant);
                TrainingType trainingType = trainingType(variant, blockType);
                String candidateKey = resource.resourceKey() + "@" + resource.semanticVersion()
                        + "#" + variant.variantKey();
                PrescriptionRankingPolicy.Factors factors = factors(
                        learner, variant, resource, skillStates, availableMinutes, feedback, superseded);
                PrescriptionRankingPolicy.Candidate candidate = new PrescriptionRankingPolicy.Candidate(
                        candidateKey,
                        variant.targetSkillKeys().stream().sorted().findFirst().orElse(variant.variantKey()),
                        blockType,
                        Math.min(resource.estimatedMinutes(), variant.duration().maximumMinutes()),
                        factors,
                        BigDecimal.ZERO);
                rankingCandidates.add(candidate);
                contexts.put(candidateKey, new CandidateContext(resource, variant, trainingType, blockType, factors));
            }
        }
        PrescriptionRankingPolicy.RankingResult ranked = rankingPolicy.rank(rankingCandidates);
        InterleavingPolicy.Decision composition = interleavingPolicy.compose(
                ranked.rankedCandidates(), availableMinutes);
        if (!composition.composable()) {
            throw PrescriptionApplicationException.noCandidate(hasGeneralFallback(accessible));
        }

        boolean fallbackAvailable = hasGeneralFallback(accessible);
        ExperienceCatalog experienceCatalog = experienceRepository.findCatalog()
                .orElseThrow(() -> PrescriptionApplicationException.noCandidate(fallbackAvailable));
        List<PrescriptionBlock> blocks = new ArrayList<>();
        int sequence = 1;
        for (PrescriptionRankingPolicy.ScoredCandidate scored : composition.blocks()) {
            CandidateContext context = contexts.get(scored.candidate().candidateKey());
            ExperienceResolutionRequest request = new ExperienceResolutionRequest(
                    context.variant().variantKey(), targetLevel, goalTags(learner.primaryGoal(), feedback),
                    topicTags(context.resource()), Set.of(interactionTag(context.trainingType())), Set.of(),
                    continuityEpisode(superseded), null);
            ExperienceResolution resolution = context.blockType() == BlockType.TRANSFER
                    ? mappingResolver.resolveInDifferentEpisode(experienceCatalog, request, continuityEpisode(superseded))
                    : mappingResolver.resolve(
                    experienceCatalog,
                    request);
            if (resolution.mapping().isEmpty()) {
                continue;
            }
            EpisodeMapping mapping = resolution.mapping().orElseThrow();
            if (mapping.resources().stream().noneMatch(reference ->
                    reference.resourceKey().equals(context.resource().resourceKey())
                            && reference.resourceVersion().equals(context.resource().semanticVersion()))) {
                continue;
            }
            blocks.add(toBlock(
                    learner,
                    context,
                    mapping,
                    scored,
                    accessible,
                    sequence++));
        }
        if (blocks.stream().noneMatch(block -> block.type() == BlockType.OUTPUT)) {
            throw PrescriptionApplicationException.noCandidate(hasGeneralFallback(accessible));
        }

        Instant generatedAt = clock.instant();
        String temporaryGoal = feedback != null ? feedback.temporaryGoal() : null;
        LearnerInputSnapshot inputSnapshot = new LearnerInputSnapshot(
                learner.profileVersion(),
                availableMinutes,
                learner.primaryGoal().name(),
                temporaryGoal,
                learner.skillStates(), learningSignal(learner));
        PrescriptionGoal goal = goal(learner.primaryGoal(), temporaryGoal);
        return new DailyLearningPrescription(
                keyGenerator.nextPrescriptionKey(),
                learner.userKey(),
                learningDate,
                learner.timezone(),
                version,
                PrescriptionStatus.ACTIVE,
                goal,
                blocks,
                rationale(goal, blocks, learner, feedback),
                reasonCodes(blocks, learner, feedback),
                PedagogicalPolicyVersion.V2_P0_1,
                inputSnapshot,
                generatedAt,
                learningDate.plusDays(1).atStartOfDay(learner.timezone()).toInstant(),
                superseded == null ? null : superseded.prescriptionId());
    }

    private boolean eligible(
            SkillUnitVariant variant,
            Map<String, PrescriptionSkillState> skillStates
    ) {
        Map<String, PrerequisitePolicy.SkillState> prerequisiteStates = skillStates.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new PrerequisitePolicy.SkillState(
                                entry.getValue().mastery(), entry.getValue().confidence())));
        if (!prerequisitePolicy.evaluate(variant.prerequisites().stream().toList(), prerequisiteStates).eligible()) {
            return false;
        }
        BigDecimal mastery = averageMastery(variant, skillStates);
        boolean higherDifficulty = variant.targetSkillKeys().stream()
                .map(skillStates::get)
                .filter(java.util.Objects::nonNull)
                .anyMatch(state -> variant.level().ordinal() > state.level().ordinal());
        BlockType type = blockType(variant);
        return masteryEligibilityPolicy.evaluate(
                        mastery,
                        type == BlockType.REVIEW,
                        higherDifficulty,
                        type == BlockType.TRANSFER)
                .eligible();
    }

    private PrescriptionBlock toBlock(
            LearnerPlanningSnapshot learner,
            CandidateContext context,
            EpisodeMapping mapping,
            PrescriptionRankingPolicy.ScoredCandidate scored,
            List<PublishedResourceCandidate> accessible,
            int sequence
    ) {
        DifficultyPolicy.Decision difficulty = difficultyPolicy.evaluate(
                new DifficultyPolicy.Input(context.variant().level(), failureSignals(learner, context.variant()),
                        easyCompletionSignals(learner, context.variant())));
        ScaffoldingLevel scaffolding = context.variant().scaffoldingLevels().contains(difficulty.scaffolding())
                ? difficulty.scaffolding()
                : context.variant().scaffoldingLevels().stream().sorted().findFirst().orElseThrow();
        PrescriptionResourceRef fallback = accessible.stream()
                .filter(other -> !other.resourceKey().equals(context.resource().resourceKey()))
                .filter(other -> other.skillUnitVariantKeys().contains(context.variant().variantKey()))
                .sorted(Comparator.comparing(PublishedResourceCandidate::resourceKey))
                .findFirst()
                .map(other -> new PrescriptionResourceRef(other.resourceKey(), other.semanticVersion()))
                .orElse(null);
        ImageAssetMetadata image = (ImageAssetMetadata) context.resource().taskHero().metadata();
        String publicUrl = context.resource().taskHero().accessScope() == AccessScope.PUBLIC
                ? mediaAccessUrlIssuer.publicUrl(context.resource().taskHero()).url().toString()
                : null;
        Map<String, BigDecimal> factorMap = factorMap(context.factors());
        return new PrescriptionBlock(
                keyGenerator.nextBlockKey(),
                sequence,
                scored.candidate().blockType(),
                context.resource().title(),
                context.variant().variantKey(),
                new PrescriptionResourceRef(
                        context.resource().resourceKey(), context.resource().semanticVersion()),
                mapping.mappingKey(),
                mapping.seasonKey(),
                mapping.episodeKey(),
                mapping.sceneKey(),
                context.variant().level(),
                scaffolding,
                context.trainingType(),
                scored.candidate().estimatedMinutes(),
                context.variant().evidenceCriteria().stream()
                        .sorted(Comparator.comparingInt(EvidenceCriterion::sequence))
                        .map(EvidenceCriterion::criterionKey)
                        .toList(),
                context.variant().completionPolicy(),
                fallback,
                factorMap,
                new PrescriptionTaskHero(
                        context.resource().taskHero().assetKey(),
                        publicUrl,
                        image.aspectRatio(),
                        BigDecimal.valueOf(image.focalPoint().x()),
                        BigDecimal.valueOf(image.focalPoint().y()),
                        image.altText()),
                PrescriptionBlockStatus.READY);
    }

    private static PrescriptionRankingPolicy.Factors factors(
            LearnerPlanningSnapshot learner,
            SkillUnitVariant variant,
            PublishedResourceCandidate resource,
            Map<String, PrescriptionSkillState> skillStates,
            int availableMinutes,
            PrescriptionFeedback feedback,
            DailyLearningPrescription superseded
    ) {
        BigDecimal goalMatch = goalMatch(learner.primaryGoal(), resource, feedback);
        BigDecimal skillGap = BigDecimal.ONE.subtract(averageMastery(variant, skillStates));
        BigDecimal reviewUrgency = reviewUrgency(learner, variant);
        BigDecimal errorMatch = errorMatch(learner, variant);
        BigDecimal difficultyFit = variant.level() == learner.currentLevel()
                ? BigDecimal.ONE : new BigDecimal("0.7000");
        BigDecimal transferValue = transferValue(learner, variant);
        BigDecimal freshness = freshness(variant, skillStates, resource, superseded);
        int minutes = Math.min(resource.estimatedMinutes(), variant.duration().maximumMinutes());
        BigDecimal timeFit = minutes <= availableMinutes
                ? BigDecimal.ONE
                : BigDecimal.valueOf(availableMinutes)
                        .divide(BigDecimal.valueOf(minutes), 4, RoundingMode.HALF_UP);
        return new PrescriptionRankingPolicy.Factors(
                goalMatch, skillGap, reviewUrgency, errorMatch, difficultyFit,
                transferValue, freshness, timeFit, new BigDecimal("0.5000"), true);
    }

    private static BigDecimal averageMastery(
            SkillUnitVariant variant,
            Map<String, PrescriptionSkillState> skillStates
    ) {
        List<BigDecimal> values = variant.targetSkillKeys().stream()
                .map(skillStates::get)
                .map(state -> state == null ? new BigDecimal("0.5000") : state.mastery())
                .toList();
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal freshness(
            SkillUnitVariant variant,
            Map<String, PrescriptionSkillState> skillStates,
            PublishedResourceCandidate resource,
            DailyLearningPrescription superseded
    ) {
        int evidence = variant.targetSkillKeys().stream()
                .map(skillStates::get)
                .filter(java.util.Objects::nonNull)
                .mapToInt(PrescriptionSkillState::evidenceCount)
                .sum();
        BigDecimal baseline = evidence == 0 ? BigDecimal.ONE
                : BigDecimal.ONE.divide(BigDecimal.valueOf(Math.min(evidence + 1L, 10L)), 4, RoundingMode.HALF_UP);
        boolean recentlyUsed = superseded != null && superseded.blocks().stream().anyMatch(block ->
                block.resource().resourceKey().equals(resource.resourceKey()));
        return recentlyUsed ? baseline.multiply(new BigDecimal("0.2500")).setScale(4, RoundingMode.HALF_UP) : baseline;
    }

    private static BigDecimal reviewUrgency(LearnerPlanningSnapshot learner, SkillUnitVariant variant) {
        if (!variant.trainingTypes().contains(TrainingType.REVIEW)) {
            return BigDecimal.ZERO;
        }
        return learner.learnerMemory().dueReviews().stream()
                .filter(review -> "SKILL".equals(review.targetType()))
                .filter(review -> variant.targetSkillKeys().contains(review.targetKey()))
                .map(LearnerMemory.DueReview::forgettingRisk)
                .max(BigDecimal::compareTo)
                .orElseGet(() -> learner.learnerMemory().dueReviews().stream()
                        .anyMatch(review -> "EXPRESSION".equals(review.targetType()))
                        ? new BigDecimal("0.7000") : new BigDecimal("0.1000"));
    }

    private static BigDecimal errorMatch(LearnerPlanningSnapshot learner, SkillUnitVariant variant) {
        return learner.learnerMemory().weakPoints().stream()
                .filter(point -> variant.targetSkillKeys().contains(point.skillKey()))
                .filter(point -> variant.commonErrorTags().contains(point.errorTag()))
                .map(point -> switch (point.severity()) {
                    case "HIGH" -> BigDecimal.ONE;
                    case "MEDIUM" -> new BigDecimal("0.7000");
                    default -> new BigDecimal("0.4000");
                })
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal transferValue(LearnerPlanningSnapshot learner, SkillUnitVariant variant) {
        if (!variant.trainingTypes().contains(TrainingType.TRANSFER)) {
            return BigDecimal.ZERO;
        }
        return learner.learnerMemory().expressions().stream()
                .anyMatch(expression -> expression.state() == cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState.INDEPENDENT
                        || expression.state() == cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState.TRANSFERRED)
                ? new BigDecimal("0.9500") : BigDecimal.ZERO;
    }

    private static int failureSignals(LearnerPlanningSnapshot learner, SkillUnitVariant variant) {
        int count = learner.learnerMemory().weakPoints().stream()
                .filter(point -> "HIGH".equals(point.severity()))
                .filter(point -> variant.targetSkillKeys().contains(point.skillKey()))
                .mapToInt(LearnerMemory.WeakPoint::frequency)
                .sum();
        return Math.min(3, count);
    }

    private static int easyCompletionSignals(LearnerPlanningSnapshot learner, SkillUnitVariant variant) {
        if (failureSignals(learner, variant) > 0) {
            return 0;
        }
        long independent = learner.learnerMemory().expressions().stream()
                .filter(expression -> expression.state() == cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState.INDEPENDENT
                        || expression.state() == cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState.TRANSFERRED)
                .count();
        return independent >= 3 ? 3 : 0;
    }

    private static BigDecimal goalMatch(
            PrimaryGoal goal,
            PublishedResourceCandidate resource,
            PrescriptionFeedback feedback
    ) {
        if (feedback != null && feedback.temporaryGoal() != null) {
            String normalized = feedback.temporaryGoal().toLowerCase(Locale.ROOT);
            String searchable = (resource.title() + " " + resource.topic() + " " + resource.scene())
                    .toLowerCase(Locale.ROOT);
            return Arrays.stream(normalized.split("\\s+"))
                    .filter(token -> token.length() >= 3)
                    .anyMatch(searchable::contains) ? BigDecimal.ONE : new BigDecimal("0.8000");
        }
        String topic = resource.topic().toLowerCase(Locale.ROOT);
        return switch (goal) {
            case WORKPLACE -> topic.contains("work") || topic.contains("tech")
                    ? BigDecimal.ONE : new BigDecimal("0.5000");
            case IELTS -> topic.contains("ielts") ? BigDecimal.ONE : new BigDecimal("0.4000");
            case GENERAL -> new BigDecimal("0.7000");
        };
    }

    private static BlockType blockType(SkillUnitVariant variant) {
        if (variant.trainingTypes().contains(TrainingType.ROLE_PLAY)
                || variant.trainingTypes().contains(TrainingType.GUIDED_SPEAKING)) {
            return BlockType.OUTPUT;
        }
        if (variant.trainingTypes().contains(TrainingType.REVIEW)) {
            return BlockType.REVIEW;
        }
        if (variant.trainingTypes().contains(TrainingType.TRANSFER)) {
            return BlockType.TRANSFER;
        }
        return BlockType.ACQUISITION;
    }

    private static TrainingType trainingType(SkillUnitVariant variant, BlockType blockType) {
        List<TrainingType> preference = switch (blockType) {
            case OUTPUT -> List.of(TrainingType.ROLE_PLAY, TrainingType.GUIDED_SPEAKING);
            case REVIEW -> List.of(TrainingType.REVIEW);
            case TRANSFER -> List.of(TrainingType.TRANSFER);
            case ACQUISITION -> List.of(TrainingType.COMPREHENSION);
        };
        return preference.stream().filter(variant.trainingTypes()::contains).findFirst()
                .orElseGet(() -> variant.trainingTypes().stream().sorted().findFirst().orElseThrow());
    }

    private static Map<String, BigDecimal> factorMap(PrescriptionRankingPolicy.Factors factors) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("GOAL_MATCH", factors.goalMatch());
        values.put("SKILL_GAP", factors.skillGap());
        values.put("REVIEW_URGENCY", factors.reviewUrgency());
        values.put("ERROR_MATCH", factors.errorMatch());
        values.put("DIFFICULTY_FIT", factors.difficultyFit());
        values.put("TRANSFER_VALUE", factors.transferValue());
        values.put("FRESHNESS", factors.freshness());
        values.put("TIME_FIT", factors.timeFit());
        values.put("USER_PREFERENCE", factors.userPreference());
        return Map.copyOf(values);
    }

    private static CefrLevel adjustedLevel(CefrLevel current, PrescriptionFeedback feedback) {
        if (feedback == null) {
            return current;
        }
        return switch (feedback.reason()) {
            case TOO_HARD -> CefrLevel.values()[Math.max(0, current.ordinal() - 1)];
            case TOO_EASY -> CefrLevel.values()[Math.min(CefrLevel.values().length - 1, current.ordinal() + 1)];
            default -> current;
        };
    }

    private static Set<String> goalTags(PrimaryGoal goal, PrescriptionFeedback feedback) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(goal.name().toLowerCase(Locale.ROOT));
        if (feedback != null && feedback.temporaryGoal() != null) {
            tags.add("temporary");
        }
        return Set.copyOf(tags);
    }

    private static Set<String> topicTags(PublishedResourceCandidate resource) {
        return Set.of(resource.topic().toLowerCase(Locale.ROOT));
    }

    private static String interactionTag(TrainingType type) {
        return switch (type) {
            case ROLE_PLAY, GUIDED_SPEAKING -> "clarification";
            case REVIEW -> "review";
            case TRANSFER -> "transfer";
            case COMPREHENSION -> "comprehension";
        };
    }

    private static String continuityEpisode(DailyLearningPrescription current) {
        return current == null || current.blocks().isEmpty() ? null : current.blocks().getFirst().episodeKey();
    }

    private static PrescriptionGoal goal(PrimaryGoal primaryGoal, String temporaryGoal) {
        if (temporaryGoal != null) {
            return new PrescriptionGoal("TEMPORARY_GOAL", temporaryGoal);
        }
        return switch (primaryGoal) {
            case WORKPLACE -> new PrescriptionGoal("WORKPLACE_COMMUNICATION", "工作英语沟通");
            case IELTS -> new PrescriptionGoal("IELTS_SPEAKING", "IELTS 口语能力");
            case GENERAL -> new PrescriptionGoal("GENERAL_COMMUNICATION", "综合英语沟通");
        };
    }

    private static String rationale(
            PrescriptionGoal goal,
            List<PrescriptionBlock> blocks,
            LearnerPlanningSnapshot learner,
            PrescriptionFeedback feedback
    ) {
        String adjustment = feedback == null || feedback.reason() == PrescriptionFeedbackReason.LEARNING_SIGNAL ? ""
                : "，并已根据你的“" + feedback.reason().name() + "”反馈重新调整";
        String memoryReason = learner.learnerMemory().dueReviews().isEmpty() ? ""
                : "，并优先安排到期复习";
        String errorReason = learner.learnerMemory().weakPoints().isEmpty() ? ""
                : "，同时针对近期错误类型";
        return "今天优先训练“" + goal.label() + "”，从当前能力缺口中选择了“"
                + blocks.getFirst().title() + "”" + memoryReason + errorReason + adjustment + "。";
    }

    private static List<String> reasonCodes(List<PrescriptionBlock> blocks, LearnerPlanningSnapshot learner,
                                            PrescriptionFeedback feedback) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        reasons.add("GOAL_MATCH");
        reasons.add("SKILL_GAP");
        if (!learner.learnerMemory().dueReviews().isEmpty()) {
            reasons.add("REVIEW_DUE");
        }
        if (!learner.learnerMemory().weakPoints().isEmpty()) {
            reasons.add("ERROR_MEMORY");
        }
        if (blocks.stream().anyMatch(block -> block.type() == BlockType.TRANSFER)) {
            reasons.add("CROSS_SCENE_TRANSFER");
        }
        if (feedback != null && feedback.reason() != PrescriptionFeedbackReason.LEARNING_SIGNAL) {
            reasons.add("USER_FEEDBACK_" + feedback.reason().name());
        }
        if (feedback != null && feedback.reason() == PrescriptionFeedbackReason.LEARNING_SIGNAL) {
            reasons.add("LEARNING_SIGNAL_RECOMPOSED");
        }
        return List.copyOf(reasons);
    }

    private static String learningSignal(LearnerPlanningSnapshot learner) {
        String skills = learner.skillStates().stream().sorted(Comparator.comparing(PrescriptionSkillState::skillKey))
                .map(state -> state.skillKey() + ":" + state.mastery() + ":" + state.confidence() + ":" + state.evidenceCount()
                        + ":" + state.lastEvidenceAt()).collect(Collectors.joining("|"));
        String errors = learner.learnerMemory().weakPoints().stream()
                .map(point -> point.errorTag() + ":" + point.skillKey() + ":" + point.frequency() + ":" + point.severity())
                .sorted().collect(Collectors.joining("|"));
        String expressions = learner.learnerMemory().expressions().stream()
                .map(expression -> expression.normalizedExpression() + ":" + expression.state() + ":" + expression.confidence())
                .sorted().collect(Collectors.joining("|"));
        String reviews = learner.learnerMemory().dueReviews().stream()
                .map(review -> review.targetType() + ":" + review.targetKey() + ":" + review.dueAt() + ":" + review.forgettingRisk())
                .sorted().collect(Collectors.joining("|"));
        return requestHash("LEARNER_SIGNAL", skills + "#" + errors + "#" + expressions + "#" + reviews);
    }

    private static boolean hasGeneralFallback(List<PublishedResourceCandidate> candidates) {
        return candidates.stream().anyMatch(candidate ->
                candidate.title().toLowerCase(Locale.ROOT).contains("conversation"));
    }

    private static void validateTimezone(String requestedTimezone, ZoneId learnerZone) {
        if (requestedTimezone == null || requestedTimezone.isBlank()) {
            return;
        }
        ZoneId requested;
        try {
            requested = ZoneId.of(requestedTimezone.strip());
        } catch (Exception exception) {
            throw new PrescriptionApplicationException(
                    "INVALID_TIMEZONE", 400, "timezone is invalid");
        }
        if (!requested.equals(learnerZone)) {
            throw new PrescriptionApplicationException(
                    "PRESCRIPTION_TIMEZONE_MISMATCH", 400, "timezone must match the learner profile");
        }
    }

    private static String idempotencyKey(String value) {
        String key = required(value, "Idempotency-Key");
        if (key.length() > 128) {
            throw new PrescriptionApplicationException(
                    "INVALID_IDEMPOTENCY_KEY", 400, "Idempotency-Key must not exceed 128 characters");
        }
        return key;
    }

    private static String requestHash(String operation, String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((operation + "\n" + value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("request hash could not be calculated", exception);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new PrescriptionApplicationException(
                    "INVALID_" + field.toUpperCase(Locale.ROOT).replace('-', '_'), 400,
                    field + " is required");
        }
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record CandidateContext(
            PublishedResourceCandidate resource,
            SkillUnitVariant variant,
            TrainingType trainingType,
            BlockType blockType,
            PrescriptionRankingPolicy.Factors factors
    ) {
    }
}
