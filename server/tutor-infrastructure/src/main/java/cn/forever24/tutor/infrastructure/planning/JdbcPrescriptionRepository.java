package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.PrescriptionApplicationException;
import cn.forever24.tutor.application.planning.PrescriptionFeedback;
import cn.forever24.tutor.application.planning.PrescriptionMutationResult;
import cn.forever24.tutor.application.planning.PrescriptionRepository;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.planning.PrescriptionBlockStatus;
import cn.forever24.tutor.planning.PrescriptionGoal;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.planning.policy.PedagogicalPolicyVersion;
import cn.forever24.tutor.profile.UserKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JdbcPrescriptionRepository implements PrescriptionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcPrescriptionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper).copy().findAndRegisterModules();
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Optional<DailyLearningPrescription> findActive(UserKey userKey, LocalDate learningDate) {
        return findOne(
                """
                        SELECT lp.*, u.user_key,
                               superseded.plan_key AS supersedes_plan_key
                        FROM learning_plan lp
                        JOIN app_user u ON u.id = lp.user_id
                        LEFT JOIN learning_plan superseded ON superseded.id = lp.supersedes_plan_id
                        WHERE u.user_key = ? AND u.status = 'ACTIVE'
                          AND lp.plan_date = ? AND lp.status = 'ACTIVE'
                          AND lp.prescription_version IS NOT NULL
                        ORDER BY lp.prescription_version DESC
                        LIMIT 1
                        """,
                userKey.value(), learningDate);
    }

    @Override
    public Optional<DailyLearningPrescription> findOwned(UserKey userKey, String prescriptionId) {
        return findOne(
                """
                        SELECT lp.*, u.user_key,
                               superseded.plan_key AS supersedes_plan_key
                        FROM learning_plan lp
                        JOIN app_user u ON u.id = lp.user_id
                        LEFT JOIN learning_plan superseded ON superseded.id = lp.supersedes_plan_id
                        WHERE u.user_key = ? AND u.status = 'ACTIVE'
                          AND lp.plan_key = ? AND lp.prescription_version IS NOT NULL
                        """,
                userKey.value(), prescriptionId);
    }

    @Override
    public Optional<PrescriptionMutationResult> findReplay(
            UserKey userKey,
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        return Optional.ofNullable(loadReplay(userKey, operation, idempotencyKey, requestHash));
    }

    @Override
    @Transactional
    public DailyLearningPrescription saveInitialIfAbsent(DailyLearningPrescription prescription) {
        Optional<DailyLearningPrescription> existing = findActive(
                prescription.userKey(), prescription.learningDate());
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        try {
            persist(prescription);
            return prescription;
        } catch (DataIntegrityViolationException exception) {
            return findActive(prescription.userKey(), prescription.learningDate()).orElseThrow(() -> exception);
        }
    }

    @Override
    @Transactional
    public PrescriptionMutationResult replaceActive(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription replacement,
            PrescriptionFeedback feedback,
            String idempotencyKey,
            String requestHash
    ) {
        PrescriptionMutationResult replay = loadReplay(
                expectedCurrent.userKey(), "REGENERATE", idempotencyKey, requestHash);
        if (replay != null) {
            return replay;
        }
        LocalDateTime now = utcNow();
        int updated = jdbcTemplate.update(
                """
                        UPDATE learning_plan lp
                        JOIN app_user u ON u.id = lp.user_id
                        SET lp.status = 'SUPERSEDED', lp.updated_at_utc = ?, lp.version = lp.version + 1
                        WHERE u.user_key = ? AND lp.plan_key = ?
                          AND lp.status = 'ACTIVE' AND lp.prescription_version = ?
                        """,
                now,
                expectedCurrent.userKey().value(),
                expectedCurrent.prescriptionId(),
                expectedCurrent.version());
        if (updated != 1) {
            throw stale();
        }
        persist(replacement);
        persistFeedback(expectedCurrent, replacement, feedback, "REGENERATE", idempotencyKey, requestHash);
        return new PrescriptionMutationResult(replacement, false);
    }

    @Override
    @Transactional
    public PrescriptionMutationResult saveBlockSkip(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription updated,
            PrescriptionFeedback feedback,
            String idempotencyKey,
            String requestHash
    ) {
        PrescriptionMutationResult replay = loadReplay(
                expectedCurrent.userKey(), "SKIP", idempotencyKey, requestHash);
        if (replay != null) {
            return replay;
        }
        int changed = jdbcTemplate.update(
                """
                        UPDATE learning_task lt
                        JOIN learning_plan lp ON lp.id = lt.plan_id
                        JOIN app_user u ON u.id = lp.user_id
                        SET lt.status = 'SKIPPED', lt.updated_at_utc = ?, lt.version = lt.version + 1
                        WHERE u.user_key = ? AND lp.plan_key = ? AND lp.status = 'ACTIVE'
                          AND lp.prescription_version = ? AND lt.task_key = ? AND lt.status = 'READY'
                        """,
                utcNow(),
                expectedCurrent.userKey().value(),
                expectedCurrent.prescriptionId(),
                expectedCurrent.version(),
                feedback.blockId());
        if (changed != 1) {
            throw stale();
        }
        persistFeedback(expectedCurrent, updated, feedback, "SKIP", idempotencyKey, requestHash);
        return new PrescriptionMutationResult(updated, false);
    }

    private Optional<DailyLearningPrescription> findOne(String sql, Object... parameters) {
        List<DailyLearningPrescription> values = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> mapPrescription(resultSet),
                parameters);
        return values.stream().findFirst();
    }

    private DailyLearningPrescription mapPrescription(ResultSet resultSet) throws SQLException {
        long planId = resultSet.getLong("id");
        List<PrescriptionBlock> blocks = jdbcTemplate.query(
                """
                        SELECT task_payload_json, status
                        FROM learning_task
                        WHERE plan_id = ?
                        ORDER BY sequence_no
                        """,
                (taskSet, rowNum) -> {
                    PrescriptionBlock block = read(taskSet.getString("task_payload_json"), PrescriptionBlock.class);
                    if (PrescriptionBlockStatus.valueOf(taskSet.getString("status")) == PrescriptionBlockStatus.SKIPPED
                            && block.status() != PrescriptionBlockStatus.SKIPPED) {
                        return block.skipped();
                    }
                    return block;
                },
                planId);
        return new DailyLearningPrescription(
                resultSet.getString("plan_key"),
                new UserKey(resultSet.getString("user_key")),
                resultSet.getObject("plan_date", LocalDate.class),
                ZoneId.of(resultSet.getString("learner_timezone")),
                resultSet.getLong("prescription_version"),
                PrescriptionStatus.valueOf(resultSet.getString("status")),
                new PrescriptionGoal(resultSet.getString("priority_goal"), resultSet.getString("focus_summary")),
                blocks,
                resultSet.getString("rationale"),
                readStringList(resultSet.getString("reason_codes_json")),
                new PedagogicalPolicyVersion(resultSet.getString("policy_version")),
                read(resultSet.getString("input_snapshot_json"), cn.forever24.tutor.planning.LearnerInputSnapshot.class),
                resultSet.getTimestamp("created_at_utc").toInstant(),
                resultSet.getTimestamp("expires_at_utc").toInstant(),
                resultSet.getString("supersedes_plan_key"));
    }

    private void persist(DailyLearningPrescription prescription) {
        long userId = userId(prescription.userKey());
        Long supersedesId = prescription.supersedesPrescriptionId() == null
                ? null
                : planId(prescription.supersedesPrescriptionId());
        LocalDateTime generatedAt = LocalDateTime.ofInstant(prescription.generatedAt(), ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                        INSERT INTO learning_plan
                            (plan_key, user_id, plan_date, plan_type, status, profile_version,
                             adjustment_version, prescription_version, learner_timezone, priority_goal,
                             policy_version, input_snapshot_json, reason_codes_json, expires_at_utc,
                             supersedes_plan_id, duration_minutes, focus_summary, rationale,
                             generation_source, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, 'DAILY_PRESCRIPTION', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                                'DETERMINISTIC_POLICY', ?, ?, 0)
                        """,
                prescription.prescriptionId(),
                userId,
                prescription.learningDate(),
                prescription.status().name(),
                prescription.inputSnapshot().profileVersion(),
                prescription.version(),
                prescription.version(),
                prescription.learnerZone().getId(),
                prescription.priorityGoal().code(),
                prescription.policyVersion().value(),
                write(prescription.inputSnapshot()),
                write(prescription.reasonCodes()),
                LocalDateTime.ofInstant(prescription.expiresAt(), ZoneOffset.UTC),
                supersedesId,
                prescription.estimatedMinutes(),
                prescription.priorityGoal().label(),
                prescription.rationale(),
                generatedAt,
                generatedAt);
        long planId = planId(prescription.prescriptionId());
        for (PrescriptionBlock block : prescription.blocks()) {
            persistBlock(planId, block, generatedAt);
        }
    }

    private void persistBlock(long planId, PrescriptionBlock block, LocalDateTime now) {
        Long variantId = requiredId(
                "SELECT id FROM curriculum_skill_unit_variant WHERE variant_key = ?",
                block.skillUnitVariantKey());
        Long resourceVersionId = resourceVersionId(block.resource().resourceKey(), block.resource().resourceVersion());
        Long mappingId = requiredId("SELECT id FROM episode_mapping WHERE mapping_key = ?", block.episodeMappingKey());
        Long fallbackId = block.fallback() == null ? null
                : resourceVersionId(block.fallback().resourceKey(), block.fallback().resourceVersion());
        jdbcTemplate.update(
                """
                        INSERT INTO learning_task
                            (task_key, plan_id, sequence_no, task_type, block_type, skill_unit_variant_id,
                             resource_version_id, episode_mapping_id, scaffolding_level, training_type,
                             expected_evidence_json, fallback_resource_version_id, recommendation_factors_json,
                             target_skills, knowledge_targets, scenario, difficulty_band, duration_minutes,
                             content_ref, task_payload_json, evidence_policy_json, status,
                             created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, 'PRESCRIPTION_BLOCK', ?, ?, ?, ?, ?, ?, ?, ?, ?,
                                JSON_ARRAY(), JSON_ARRAY(), ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                block.blockId(),
                planId,
                block.sequence(),
                block.type().name(),
                variantId,
                resourceVersionId,
                mappingId,
                block.scaffolding().name(),
                block.trainingType().name(),
                write(block.expectedEvidence()),
                fallbackId,
                write(block.recommendationFactors()),
                block.sceneKey(),
                block.difficulty().name(),
                block.estimatedMinutes(),
                block.resource().resourceKey() + "@" + block.resource().resourceVersion(),
                write(block),
                write(block.completionPolicy()),
                block.status().name(),
                now,
                now);
    }

    private PrescriptionMutationResult loadReplay(
            UserKey userKey,
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        List<ReplayRow> rows = jdbcTemplate.query(
                """
                        SELECT pf.request_hash, response.plan_key
                        FROM prescription_feedback pf
                        JOIN app_user u ON u.id = pf.user_id
                        JOIN learning_plan response ON response.id = pf.response_plan_id
                        WHERE u.user_key = ? AND pf.operation = ? AND pf.idempotency_key = ?
                        """,
                (resultSet, rowNum) -> new ReplayRow(
                        resultSet.getString("request_hash"), resultSet.getString("plan_key")),
                userKey.value(), operation, idempotencyKey);
        if (rows.isEmpty()) {
            return null;
        }
        ReplayRow row = rows.getFirst();
        if (!row.requestHash().equals(requestHash)) {
            throw new PrescriptionApplicationException(
                    "IDEMPOTENCY_CONFLICT", 409, "Idempotency-Key was already used for another request");
        }
        DailyLearningPrescription response = findOwned(userKey, row.responsePlanKey()).orElseThrow();
        return new PrescriptionMutationResult(response, true);
    }

    private void persistFeedback(
            DailyLearningPrescription expectedCurrent,
            DailyLearningPrescription response,
            PrescriptionFeedback feedback,
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO prescription_feedback
                            (feedback_key, user_id, plan_id, block_id, operation, feedback_type,
                             available_minutes, temporary_goal, note, idempotency_key, request_hash,
                             response_plan_id, created_at_utc)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "feedback-" + UUID.randomUUID(),
                userId(expectedCurrent.userKey()),
                planId(expectedCurrent.prescriptionId()),
                feedback.blockId(),
                operation,
                feedback.reason().name(),
                feedback.availableMinutes(),
                feedback.temporaryGoal(),
                feedback.note(),
                idempotencyKey,
                requestHash,
                planId(response.prescriptionId()),
                utcNow());
    }

    private long userId(UserKey userKey) {
        return requiredId("SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE'", userKey.value());
    }

    private long planId(String planKey) {
        return requiredId("SELECT id FROM learning_plan WHERE plan_key = ?", planKey);
    }

    private long resourceVersionId(String resourceKey, String semanticVersion) {
        return requiredId(
                """
                        SELECT rv.id FROM learning_resource_version rv
                        JOIN learning_resource r ON r.id = rv.resource_id
                        WHERE r.resource_key = ? AND rv.semantic_version = ?
                        """,
                resourceKey, semanticVersion);
    }

    private long requiredId(String sql, Object... parameters) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, parameters);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalStateException("prescription reference does not exist", exception);
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("prescription JSON serialization failed", exception);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("prescription JSON deserialization failed", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("prescription JSON deserialization failed", exception);
        }
    }

    private static PrescriptionApplicationException stale() {
        return new PrescriptionApplicationException(
                "PRESCRIPTION_STALE", 409, "prescription is no longer active");
    }

    private record ReplayRow(String requestHash, String responsePlanKey) {
    }
}
