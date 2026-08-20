package cn.forever24.tutor.infrastructure.curriculum;

import cn.forever24.tutor.application.curriculum.CurriculumRepository;
import cn.forever24.tutor.application.curriculum.CurriculumVariantQuery;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CefrRange;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.CurriculumCatalog;
import cn.forever24.tutor.curriculum.CurriculumStatus;
import cn.forever24.tutor.curriculum.DurationRange;
import cn.forever24.tutor.curriculum.EvidenceCriterion;
import cn.forever24.tutor.curriculum.MasteryImpactPolicy;
import cn.forever24.tutor.curriculum.Prerequisite;
import cn.forever24.tutor.curriculum.RetryPolicy;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.SkillNode;
import cn.forever24.tutor.curriculum.SkillUnit;
import cn.forever24.tutor.curriculum.SkillUnitVariant;
import cn.forever24.tutor.curriculum.TrainingType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JdbcCurriculumRepository implements CurriculumRepository {

    private static final TypeReference<List<TrainingType>> TRAINING_TYPES = new TypeReference<>() { };
    private static final TypeReference<List<ScaffoldingLevel>> SCAFFOLDING_LEVELS = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcCurriculumRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void replace(CurriculumCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("catalog is required");
        }
        jdbcTemplate.update("DELETE FROM curriculum_skill_unit");
        jdbcTemplate.update("DELETE FROM curriculum_skill");

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Map<String, Long> skillIds = persistSkills(catalog.skills(), now);
        persistParentEdges(catalog.skills(), skillIds);
        for (SkillUnit unit : catalog.skillUnits()) {
            long unitId = insertUnit(unit, now);
            for (SkillUnitVariant variant : unit.variants()) {
                long variantId = insertVariant(unitId, variant);
                persistVariantSkills(variantId, variant, skillIds);
                persistPrerequisites(variantId, variant.prerequisites(), skillIds);
                persistEvidenceCriteria(variantId, variant.evidenceCriteria());
            }
        }
    }

    @Override
    public Optional<SkillNode> findSkill(String skillKey) {
        return findSkills().stream().filter(skill -> skill.skillKey().equals(skillKey)).findFirst();
    }

    @Override
    public List<SkillNode> findSkills() {
        return jdbcTemplate.query("""
                        SELECT s.skill_key, s.name, s.category, p.skill_key AS parent_skill_key,
                               s.cefr_min, s.cefr_max, s.importance, s.status
                        FROM curriculum_skill s
                        LEFT JOIN curriculum_skill_edge e
                          ON e.child_skill_id = s.id AND e.edge_type = 'PARENT'
                        LEFT JOIN curriculum_skill p ON p.id = e.parent_skill_id
                        ORDER BY s.skill_key
                        """,
                (resultSet, rowNum) -> new SkillNode(
                        resultSet.getString("skill_key"),
                        resultSet.getString("name"),
                        resultSet.getString("category"),
                        resultSet.getString("parent_skill_key"),
                        new CefrRange(
                                CefrLevel.valueOf(resultSet.getString("cefr_min")),
                                CefrLevel.valueOf(resultSet.getString("cefr_max"))),
                        resultSet.getInt("importance"),
                        CurriculumStatus.valueOf(resultSet.getString("status"))));
    }

    @Override
    public List<SkillUnitVariant> findVariants(CurriculumVariantQuery query) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT v.id
                FROM curriculum_skill_unit_variant v
                JOIN curriculum_skill_unit u ON u.id = v.skill_unit_id
                WHERE v.status = ?
                  AND u.status = 'ACTIVE'
                  AND NOT EXISTS (
                    SELECT 1
                    FROM curriculum_variant_target_skill blocked_link
                    JOIN curriculum_skill blocked_skill ON blocked_skill.id = blocked_link.skill_id
                    WHERE blocked_link.variant_id = v.id
                      AND blocked_link.role = 'TARGET'
                      AND blocked_skill.status <> 'ACTIVE'
                  )
                """);
        List<Object> arguments = new ArrayList<>();
        arguments.add(query.status().name());
        if (query.level() != null) {
            sql.append(" AND v.cefr_level = ?");
            arguments.add(query.level().name());
        }
        if (query.skillKey() != null) {
            sql.append("""
                     AND EXISTS (
                       SELECT 1
                       FROM curriculum_variant_target_skill requested_link
                       JOIN curriculum_skill requested_skill ON requested_skill.id = requested_link.skill_id
                       WHERE requested_link.variant_id = v.id
                         AND requested_skill.skill_key = ?
                         AND requested_skill.status = 'ACTIVE'
                     )
                    """);
            arguments.add(query.skillKey());
        }
        sql.append(" ORDER BY v.id");
        return jdbcTemplate.query(sql.toString(),
                        (resultSet, rowNum) -> resultSet.getLong("id"),
                        arguments.toArray()).stream()
                .map(this::loadVariant)
                .sorted(Comparator.comparing(SkillUnitVariant::variantKey))
                .toList();
    }

    private Map<String, Long> persistSkills(List<SkillNode> skills, LocalDateTime now) {
        java.util.LinkedHashMap<String, Long> ids = new java.util.LinkedHashMap<>();
        for (SkillNode skill : skills) {
            long id = insertAndReturnId(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO curriculum_skill (
                            skill_key, name, category, cefr_min, cefr_max, importance, status,
                            created_at_utc, updated_at_utc, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, skill.skillKey());
                statement.setString(2, skill.name());
                statement.setString(3, skill.category());
                statement.setString(4, skill.cefrRange().minimum().name());
                statement.setString(5, skill.cefrRange().maximum().name());
                statement.setInt(6, skill.importance());
                statement.setString(7, skill.status().name());
                statement.setObject(8, now);
                statement.setObject(9, now);
                return statement;
            });
            ids.put(skill.skillKey(), id);
        }
        return Map.copyOf(ids);
    }

    private void persistParentEdges(List<SkillNode> skills, Map<String, Long> skillIds) {
        for (SkillNode skill : skills) {
            if (skill.parentSkillKey() != null) {
                jdbcTemplate.update("""
                                INSERT INTO curriculum_skill_edge (parent_skill_id, child_skill_id, edge_type)
                                VALUES (?, ?, 'PARENT')
                                """,
                        skillIds.get(skill.parentSkillKey()),
                        skillIds.get(skill.skillKey()));
            }
        }
    }

    private long insertUnit(SkillUnit unit, LocalDateTime now) {
        return insertAndReturnId(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO curriculum_skill_unit (
                        skill_unit_key, communication_goal, review_template_json, semantic_version,
                        status, created_at_utc, updated_at_utc, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, unit.skillUnitKey());
            statement.setString(2, unit.communicationGoal());
            statement.setString(3, toJson(unit.reviewTemplate()));
            statement.setString(4, unit.semanticVersion());
            statement.setString(5, unit.status().name());
            statement.setObject(6, now);
            statement.setObject(7, now);
            return statement;
        });
    }

    private long insertVariant(long unitId, SkillUnitVariant variant) {
        return insertAndReturnId(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO curriculum_skill_unit_variant (
                        variant_key, skill_unit_id, cefr_level, communication_complexity,
                        estimated_min_minutes, estimated_max_minutes, training_types_json,
                        scaffolding_levels_json, common_error_tags_json, completion_policy_json,
                        retry_policy_json, mastery_policy_json, status, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, variant.variantKey());
            statement.setLong(2, unitId);
            statement.setString(3, variant.level().name());
            statement.setInt(4, variant.communicationComplexity());
            statement.setInt(5, variant.duration().minimumMinutes());
            statement.setInt(6, variant.duration().maximumMinutes());
            statement.setString(7, toJson(variant.trainingTypes()));
            statement.setString(8, toJson(variant.scaffoldingLevels()));
            statement.setString(9, toJson(variant.commonErrorTags()));
            statement.setString(10, toJson(variant.completionPolicy()));
            statement.setString(11, toJson(variant.retryPolicy()));
            statement.setString(12, toJson(variant.masteryImpactPolicy()));
            statement.setString(13, variant.status().name());
            return statement;
        });
    }

    private void persistVariantSkills(long variantId, SkillUnitVariant variant, Map<String, Long> skillIds) {
        variant.targetSkillKeys().forEach(skill -> insertVariantSkill(variantId, skillIds.get(skill), "TARGET"));
        variant.supportingSkillKeys().forEach(skill -> insertVariantSkill(variantId, skillIds.get(skill), "SUPPORTING"));
    }

    private void insertVariantSkill(long variantId, long skillId, String role) {
        jdbcTemplate.update("""
                        INSERT INTO curriculum_variant_target_skill (variant_id, skill_id, role)
                        VALUES (?, ?, ?)
                        """,
                variantId, skillId, role);
    }

    private void persistPrerequisites(long variantId, Set<Prerequisite> prerequisites, Map<String, Long> skillIds) {
        for (Prerequisite prerequisite : prerequisites) {
            jdbcTemplate.update("""
                            INSERT INTO curriculum_variant_prerequisite (
                                variant_id, skill_id, minimum_mastery, minimum_confidence
                            ) VALUES (?, ?, ?, ?)
                            """,
                    variantId,
                    skillIds.get(prerequisite.skillKey()),
                    prerequisite.minimumMastery(),
                    prerequisite.minimumConfidence());
        }
    }

    private void persistEvidenceCriteria(long variantId, List<EvidenceCriterion> criteria) {
        for (EvidenceCriterion criterion : criteria) {
            jdbcTemplate.update("""
                            INSERT INTO curriculum_evidence_criterion (
                                criterion_key, variant_id, description, weight, required, sequence_no
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    criterion.criterionKey(),
                    variantId,
                    criterion.description(),
                    criterion.weight(),
                    criterion.required(),
                    criterion.sequence());
        }
    }

    private SkillUnitVariant loadVariant(long variantId) {
        VariantRow row = jdbcTemplate.queryForObject("""
                        SELECT variant_key, cefr_level, communication_complexity,
                               estimated_min_minutes, estimated_max_minutes, training_types_json,
                               scaffolding_levels_json, common_error_tags_json, completion_policy_json,
                               retry_policy_json, mastery_policy_json, status
                        FROM curriculum_skill_unit_variant
                        WHERE id = ?
                        """,
                (resultSet, rowNum) -> new VariantRow(
                        resultSet.getString("variant_key"),
                        CefrLevel.valueOf(resultSet.getString("cefr_level")),
                        resultSet.getInt("communication_complexity"),
                        resultSet.getInt("estimated_min_minutes"),
                        resultSet.getInt("estimated_max_minutes"),
                        resultSet.getString("training_types_json"),
                        resultSet.getString("scaffolding_levels_json"),
                        resultSet.getString("common_error_tags_json"),
                        resultSet.getString("completion_policy_json"),
                        resultSet.getString("retry_policy_json"),
                        resultSet.getString("mastery_policy_json"),
                        CurriculumStatus.valueOf(resultSet.getString("status"))),
                variantId);
        Map<String, Set<String>> skillsByRole = loadVariantSkills(variantId);
        return new SkillUnitVariant(
                row.variantKey(),
                row.level(),
                row.communicationComplexity(),
                new DurationRange(row.minimumMinutes(), row.maximumMinutes()),
                new LinkedHashSet<>(fromJson(row.trainingTypesJson(), TRAINING_TYPES)),
                new LinkedHashSet<>(fromJson(row.scaffoldingLevelsJson(), SCAFFOLDING_LEVELS)),
                new LinkedHashSet<>(fromJson(row.commonErrorTagsJson(), STRINGS)),
                skillsByRole.getOrDefault("TARGET", Set.of()),
                skillsByRole.getOrDefault("SUPPORTING", Set.of()),
                loadPrerequisites(variantId),
                loadEvidenceCriteria(variantId),
                fromJson(row.completionPolicyJson(), CompletionPolicy.class),
                fromJson(row.retryPolicyJson(), RetryPolicy.class),
                fromJson(row.masteryPolicyJson(), MasteryImpactPolicy.class),
                row.status());
    }

    private Map<String, Set<String>> loadVariantSkills(long variantId) {
        java.util.LinkedHashMap<String, Set<String>> byRole = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT link.role, skill.skill_key
                        FROM curriculum_variant_target_skill link
                        JOIN curriculum_skill skill ON skill.id = link.skill_id
                        WHERE link.variant_id = ?
                        ORDER BY link.role, skill.skill_key
                        """,
                (RowCallbackHandler) resultSet -> byRole
                        .computeIfAbsent(resultSet.getString("role"), ignored -> new LinkedHashSet<>())
                        .add(resultSet.getString("skill_key")),
                variantId);
        return Map.copyOf(byRole);
    }

    private Set<Prerequisite> loadPrerequisites(long variantId) {
        return new LinkedHashSet<>(jdbcTemplate.query("""
                        SELECT skill.skill_key, prerequisite.minimum_mastery, prerequisite.minimum_confidence
                        FROM curriculum_variant_prerequisite prerequisite
                        JOIN curriculum_skill skill ON skill.id = prerequisite.skill_id
                        WHERE prerequisite.variant_id = ?
                        ORDER BY skill.skill_key
                        """,
                (resultSet, rowNum) -> new Prerequisite(
                        resultSet.getString("skill_key"),
                        resultSet.getBigDecimal("minimum_mastery"),
                        resultSet.getBigDecimal("minimum_confidence")),
                variantId));
    }

    private List<EvidenceCriterion> loadEvidenceCriteria(long variantId) {
        return jdbcTemplate.query("""
                        SELECT criterion_key, description, weight, required, sequence_no
                        FROM curriculum_evidence_criterion
                        WHERE variant_id = ?
                        ORDER BY sequence_no, criterion_key
                        """,
                (resultSet, rowNum) -> new EvidenceCriterion(
                        resultSet.getString("criterion_key"),
                        resultSet.getString("description"),
                        resultSet.getBigDecimal("weight"),
                        resultSet.getBoolean("required"),
                        resultSet.getInt("sequence_no")),
                variantId);
    }

    private long insertAndReturnId(PreparedStatementCreator creator) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(creator, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("database did not return a generated curriculum id");
        }
        return key.longValue();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("curriculum value could not be serialized", exception);
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("curriculum value could not be deserialized", exception);
        }
    }

    private <T> T fromJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("curriculum value could not be deserialized", exception);
        }
    }

    private record VariantRow(
            String variantKey,
            CefrLevel level,
            int communicationComplexity,
            int minimumMinutes,
            int maximumMinutes,
            String trainingTypesJson,
            String scaffoldingLevelsJson,
            String commonErrorTagsJson,
            String completionPolicyJson,
            String retryPolicyJson,
            String masteryPolicyJson,
            CurriculumStatus status
    ) { }
}
