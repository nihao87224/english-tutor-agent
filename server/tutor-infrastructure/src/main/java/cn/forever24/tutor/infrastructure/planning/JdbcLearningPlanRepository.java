package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.planning.LearnerSkillState;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanContext;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.planning.RuleBasedTodayPlanGenerator;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class JdbcLearningPlanRepository implements LearningPlanRepository {

    private static final int ADJUSTMENT_VERSION = 0;
    private static final String CONTENT_REF = "rule-plan-content-v1";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JdbcLearningPlanRepository(JdbcTemplate jdbcTemplate, Clock clock, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
        PlanProfile profile = findProfile(userKey);
        LearningPlan existing = findExistingPlan(profile.userId(), planDate, profile.profileVersion());
        if (existing != null) {
            return existing;
        }

        List<LearnerSkillState> skillStates = findSkillStates(profile.userId());
        if (skillStates.isEmpty()) {
            throw new IllegalArgumentException("initial learner profile is required before planning");
        }
        LearningPlan plan = RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                "plan-" + UUID.randomUUID(),
                planDate,
                profile.primaryGoal(),
                profile.dailyMinutes(),
                profile.profileVersion(),
                skillStates));
        persistPlan(profile.userId(), plan);
        return plan;
    }

    @Override
    public LearningPlan getPlan(UserKey userKey, String planId) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        try {
            PlanRow planRow = jdbcTemplate.queryForObject(
                    """
                            SELECT lp.id, lp.plan_key, lp.plan_date, lp.duration_minutes,
                                   lp.rationale, lp.profile_version
                            FROM learning_plan lp
                            JOIN app_user u ON u.id = lp.user_id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND lp.plan_key = ?
                            """,
                    (resultSet, rowNum) -> new PlanRow(
                            resultSet.getLong("id"),
                            resultSet.getString("plan_key"),
                            resultSet.getObject("plan_date", LocalDate.class),
                            resultSet.getInt("duration_minutes"),
                            resultSet.getString("rationale"),
                            resultSet.getLong("profile_version")),
                    userKey.value(),
                    planId);
            return new LearningPlan(
                    planRow.planKey(),
                    planRow.planDate(),
                    planRow.durationMinutes(),
                    findTasks(planRow.id()),
                    deserializeReasons(planRow.rationale()),
                    false,
                    planRow.profileVersion());
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("learning plan was not found");
        }
    }

    @Override
    @Transactional
    public void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        if (practicedSkills == null || practicedSkills.isEmpty() || evidenceCount <= 0) {
            throw new IllegalArgumentException("accepted learning evidence is required before planning can change");
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        int updated = jdbcTemplate.update("""
                        UPDATE learning_plan lp
                        JOIN app_user u ON u.id = lp.user_id
                        SET lp.status = 'COMPLETED',
                            lp.updated_at_utc = ?,
                            lp.version = lp.version + 1
                        WHERE u.user_key = ?
                          AND u.status = 'ACTIVE'
                          AND lp.plan_key = ?
                          AND lp.status = 'READY'
                        """,
                now,
                userKey.value(),
                planId);
        if (updated == 0) {
            if (!planExistsForUser(userKey, planId)) {
                throw new IllegalArgumentException("learning plan was not found");
            }
            return;
        }
        jdbcTemplate.update("""
                        UPDATE user_learning_profile p
                        JOIN app_user u ON u.id = p.user_id
                        SET p.profile_version = p.profile_version + 1,
                            p.updated_at_utc = ?,
                            p.version = p.version + 1
                        WHERE u.user_key = ?
                          AND u.status = 'ACTIVE'
                        """,
                now,
                userKey.value());
    }

    private PlanProfile findProfile(UserKey userKey) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT u.id, p.primary_goal, p.daily_minutes, p.profile_version
                            FROM app_user u
                            JOIN user_learning_profile p ON p.user_id = u.id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND p.onboarding_status IN ('RESULT', 'COMPLETE')
                            """,
                    (resultSet, rowNum) -> new PlanProfile(
                            resultSet.getLong("id"),
                            PrimaryGoal.valueOf(resultSet.getString("primary_goal")),
                            resultSet.getInt("daily_minutes"),
                            resultSet.getLong("profile_version")),
                    userKey.value());
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("initial assessment result is required before planning");
        }
    }

    private List<LearnerSkillState> findSkillStates(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT dimension, estimate, confidence, level, evidence_count
                        FROM learner_skill_state
                        WHERE user_id = ?
                        ORDER BY estimate ASC, confidence DESC
                        """,
                (resultSet, rowNum) -> new LearnerSkillState(
                        resultSet.getString("dimension"),
                        resultSet.getBigDecimal("estimate"),
                        resultSet.getBigDecimal("confidence"),
                        resultSet.getString("level"),
                        resultSet.getInt("evidence_count")),
                userId);
    }

    private boolean planExistsForUser(UserKey userKey, String planId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM learning_plan lp
                        JOIN app_user u ON u.id = lp.user_id
                        WHERE u.user_key = ?
                          AND u.status = 'ACTIVE'
                          AND lp.plan_key = ?
                        """,
                Integer.class,
                userKey.value(),
                planId);
        return count != null && count > 0;
    }

    private LearningPlan findExistingPlan(long userId, LocalDate planDate, long profileVersion) {
        try {
            PlanRow planRow = jdbcTemplate.queryForObject(
                    """
                            SELECT id, plan_key, plan_date, duration_minutes, rationale, profile_version
                            FROM learning_plan
                            WHERE user_id = ?
                              AND plan_date = ?
                              AND profile_version = ?
                              AND adjustment_version = ?
                              AND status = 'READY'
                            ORDER BY id DESC
                            LIMIT 1
                            """,
                    (resultSet, rowNum) -> new PlanRow(
                            resultSet.getLong("id"),
                            resultSet.getString("plan_key"),
                            resultSet.getObject("plan_date", LocalDate.class),
                            resultSet.getInt("duration_minutes"),
                            resultSet.getString("rationale"),
                            resultSet.getLong("profile_version")),
                    userId,
                    planDate,
                    profileVersion,
                    ADJUSTMENT_VERSION);
            return new LearningPlan(
                    planRow.planKey(),
                    planRow.planDate(),
                    planRow.durationMinutes(),
                    findTasks(planRow.id()),
                    deserializeReasons(planRow.rationale()),
                    false,
                    planRow.profileVersion());
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private List<LearningPlanTask> findTasks(long planId) {
        return jdbcTemplate.query(
                """
                        SELECT task_key, task_type, task_payload_json, duration_minutes,
                               target_skills, difficulty_band
                        FROM learning_task
                        WHERE plan_id = ?
                        ORDER BY sequence_no
                        """,
                (resultSet, rowNum) -> new LearningPlanTask(
                        resultSet.getString("task_key"),
                        resultSet.getString("task_type"),
                        readJsonField(resultSet.getString("task_payload_json"), "title"),
                        resultSet.getInt("duration_minutes"),
                        deserializeStringList(resultSet.getString("target_skills")),
                        resultSet.getString("difficulty_band"),
                        readJsonField(resultSet.getString("task_payload_json"), "reason")),
                planId);
    }

    private void persistPlan(long userId, LearningPlan plan) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO learning_plan
                            (plan_key, user_id, plan_date, plan_type, status, profile_version,
                             adjustment_version, duration_minutes, focus_summary, rationale,
                             generation_source, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, 'DAILY', 'READY', ?, ?, ?, ?, ?, 'RULE', ?, ?, 0)
                        """,
                plan.planId(),
                userId,
                plan.date(),
                plan.profileVersion(),
                ADJUSTMENT_VERSION,
                plan.totalMinutes(),
                plan.tasks().get(0).title(),
                serialize(plan.reasons()),
                now,
                now);
        long planRowId = jdbcTemplate.queryForObject(
                "SELECT id FROM learning_plan WHERE plan_key = ?",
                Long.class,
                plan.planId());
        for (int index = 0; index < plan.tasks().size(); index++) {
            persistTask(planRowId, plan.tasks().get(index), index + 1, now);
        }
    }

    private void persistTask(long planId, LearningPlanTask task, int sequenceNo, LocalDateTime now) {
        jdbcTemplate.update("""
                        INSERT INTO learning_task
                            (task_key, plan_id, sequence_no, task_type, target_skills, knowledge_targets,
                             scenario, difficulty_band, duration_minutes, content_ref, task_payload_json,
                             evidence_policy_json, status, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, JSON_ARRAY(), NULL, ?, ?, ?, ?, ?, 'READY', ?, ?, 0)
                        """,
                task.taskId(),
                planId,
                sequenceNo,
                task.type(),
                serialize(task.skillFocus()),
                task.difficulty(),
                task.durationMinutes(),
                CONTENT_REF,
                serialize(java.util.Map.of("title", task.title(), "reason", task.reason())),
                serialize(java.util.Map.of("requiresEvidence", true)),
                now,
                now);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("plan JSON serialization failed", exception);
        }
    }

    private List<String> deserializeReasons(String json) {
        return deserializeStringList(json);
    }

    private List<String> deserializeStringList(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("plan JSON deserialization failed", exception);
        }
    }

    private String readJsonField(String json, String fieldName) {
        try {
            return objectMapper.readTree(json).path(fieldName).asText();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("plan JSON deserialization failed", exception);
        }
    }

    private record PlanProfile(long userId, PrimaryGoal primaryGoal, int dailyMinutes, long profileVersion) {
    }

    private record PlanRow(
            long id,
            String planKey,
            LocalDate planDate,
            int durationMinutes,
            String rationale,
            long profileVersion
    ) {
    }
}
