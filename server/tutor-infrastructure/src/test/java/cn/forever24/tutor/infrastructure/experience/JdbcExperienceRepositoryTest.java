package cn.forever24.tutor.infrastructure.experience;

import cn.forever24.tutor.experience.ExperienceCatalog;
import cn.forever24.tutor.experience.MappingResourceReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcExperienceRepositoryTest {

    private JdbcExperienceRepository repository;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:experience-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("curriculum-schema-h2.sql"),
                new ClassPathResource("resource-catalog-schema-h2.sql"),
                new ClassPathResource("experience-graph-schema-h2.sql")).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        seedReferences(SeasonOneExperienceFixture.catalog());
        repository = new JdbcExperienceRepository(
                jdbcTemplate,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void roundTripsSeasonEpisodeSceneMappingsFallbackAndResources() {
        transactionTemplate.executeWithoutResult(ignored -> repository.replace(
                SeasonOneExperienceFixture.catalog()));

        ExperienceCatalog stored = repository.findCatalog().orElseThrow();

        assertEquals(1, stored.seasons().size());
        assertEquals(3, stored.episodes().size());
        assertEquals(3, stored.scenes().size());
        assertEquals(5, stored.mappings().size());
        assertEquals(
                "s01.ep002.confirm-information.b1",
                stored.mappings().stream()
                        .filter(mapping -> mapping.mappingKey().equals("s01.ep006.confirm-information.b1"))
                        .findFirst().orElseThrow().fallbackMappingKey());
        assertEquals(
                "season1.ep006.gate-change.b1",
                stored.mappings().stream()
                        .filter(mapping -> mapping.mappingKey().equals("s01.ep006.confirm-information.b1"))
                        .findFirst().orElseThrow().resources().getFirst().resourceKey());
    }

    @Test
    void invalidResourceReferenceRollsBackReplacement() {
        ExperienceCatalog valid = SeasonOneExperienceFixture.catalog();
        transactionTemplate.executeWithoutResult(ignored -> repository.replace(valid));
        ExperienceCatalog invalid = replaceFirstResource(valid, "missing.resource.version");

        assertThrows(IllegalArgumentException.class, () -> transactionTemplate.executeWithoutResult(
                ignored -> repository.replace(invalid)));

        ExperienceCatalog stored = repository.findCatalog().orElseThrow();
        assertEquals(5, stored.mappings().size());
        assertTrue(stored.mappings().stream()
                .flatMap(mapping -> mapping.resources().stream())
                .noneMatch(reference -> reference.resourceKey().equals("missing.resource.version")));
    }

    @Test
    void outerTransactionRollsBackWholeGraphReplacement() {
        ExperienceCatalog catalog = SeasonOneExperienceFixture.catalog();

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(ignored -> {
            repository.replace(catalog);
            throw new IllegalStateException("simulate failure after persistence");
        }));

        assertTrue(repository.findCatalog().isEmpty());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM experience_season", Integer.class));
    }

    private ExperienceCatalog replaceFirstResource(ExperienceCatalog catalog, String resourceKey) {
        var mappings = new java.util.ArrayList<>(catalog.mappings());
        var original = mappings.getFirst();
        mappings.set(0, new cn.forever24.tutor.experience.EpisodeMapping(
                original.mappingKey(), original.skillUnitVariantKey(), original.seasonKey(),
                original.episodeKey(), original.sceneKey(), original.eligibleLevels(),
                original.storyTransition(), original.fitInputs(), original.fallbackMappingKey(),
                original.status(), java.util.List.of(new MappingResourceReference(resourceKey, "1.0.0", 0))));
        return new ExperienceCatalog(catalog.seasons(), catalog.episodes(), catalog.scenes(), mappings);
    }

    private void seedReferences(ExperienceCatalog catalog) {
        jdbcTemplate.update("""
                INSERT INTO content_provider (provider_code, display_name, provider_type)
                VALUES ('english-tutor-agent', 'English Tutor Agent', 'INTERNAL')
                """);
        jdbcTemplate.update("""
                INSERT INTO resource_collection (
                    collection_key, provider_code, title, access_scope, status, ownership_type,
                    allowed_audience, created_at_utc, updated_at_utc, version
                ) VALUES (
                    'INTERNAL_SCENARIO_LIBRARY', 'english-tutor-agent', 'Scenario Library',
                    'PUBLIC', 'ACTIVE', 'INTERNAL', 'ALL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
                )
                """);
        long collectionId = jdbcTemplate.queryForObject(
                "SELECT id FROM resource_collection WHERE collection_key = 'INTERNAL_SCENARIO_LIBRARY'",
                Long.class);

        Map<String, Long> variantIds = new LinkedHashMap<>();
        catalog.mappings().forEach(mapping -> variantIds.computeIfAbsent(
                mapping.skillUnitVariantKey(),
                key -> insertVariant(key, mapping.eligibleLevels().iterator().next().name())));

        for (var mapping : catalog.mappings()) {
            for (MappingResourceReference reference : mapping.resources()) {
                long resourceId = insertResource(reference.resourceKey(), collectionId);
                long versionId = insertResourceVersion(resourceId, reference.resourceVersion());
                jdbcTemplate.update("""
                                INSERT INTO resource_version_skill_variant (resource_version_id, variant_id)
                                VALUES (?, ?)
                                """,
                        versionId,
                        variantIds.get(mapping.skillUnitVariantKey()));
            }
        }
    }

    private long insertVariant(String variantKey, String level) {
        jdbcTemplate.update("""
                        INSERT INTO curriculum_skill_unit (
                            skill_unit_key, communication_goal, review_template_json, semantic_version,
                            status, created_at_utc, updated_at_utc, version
                        ) VALUES (?, ?, '{}', '1.0.0', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                        """,
                "unit." + variantKey,
                "Communication goal for " + variantKey);
        long unitId = jdbcTemplate.queryForObject(
                "SELECT id FROM curriculum_skill_unit WHERE skill_unit_key = ?",
                Long.class,
                "unit." + variantKey);
        jdbcTemplate.update("""
                        INSERT INTO curriculum_skill_unit_variant (
                            variant_key, skill_unit_id, cefr_level, communication_complexity,
                            estimated_min_minutes, estimated_max_minutes, training_types_json,
                            scaffolding_levels_json, common_error_tags_json, completion_policy_json,
                            retry_policy_json, mastery_policy_json, status, version
                        ) VALUES (?, ?, ?, 2, 8, 15, '[]', '[]', '[]', '{}', '{}', '{}', 'ACTIVE', 0)
                        """,
                variantKey,
                unitId,
                level);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM curriculum_skill_unit_variant WHERE variant_key = ?",
                Long.class,
                variantKey);
    }

    private long insertResource(String resourceKey, long collectionId) {
        jdbcTemplate.update("""
                        INSERT INTO learning_resource (
                            resource_key, provider_code, collection_id, resource_type, title, description,
                            language, level, topic, scene, communication_goal, access_scope, publish_status,
                            active_version_id, estimated_minutes, created_at_utc, updated_at_utc, version
                        ) VALUES (?, 'english-tutor-agent', ?, 'SCENARIO_LESSON', ?, '', 'en', 'B1',
                                  'General', 'SCENE', 'Practice English communication', 'PUBLIC',
                                  'PUBLISHED', NULL, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                        """,
                resourceKey,
                collectionId,
                resourceKey);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM learning_resource WHERE resource_key = ?", Long.class, resourceKey);
    }

    private long insertResourceVersion(long resourceId, String semanticVersion) {
        jdbcTemplate.update("""
                        INSERT INTO learning_resource_version (
                            resource_id, semantic_version, manifest_hash, manifest_json, learner_fit_json,
                            generation_metadata_json, created_at_utc, published_at_utc, status, version
                        ) VALUES (?, ?, ?, '{}', '{}', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 0)
                        """,
                resourceId,
                semanticVersion,
                "sha256:" + UUID.randomUUID().toString().replace("-", "") + "00000000000000000000000000000000");
        return jdbcTemplate.queryForObject("""
                        SELECT id FROM learning_resource_version
                        WHERE resource_id = ? AND semantic_version = ?
                        """,
                Long.class,
                resourceId,
                semanticVersion);
    }
}
