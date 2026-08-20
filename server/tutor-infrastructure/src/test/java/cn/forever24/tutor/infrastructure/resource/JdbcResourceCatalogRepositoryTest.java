package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.CatalogWriteOutcome;
import cn.forever24.tutor.application.resource.ResourceCandidateQuery;
import cn.forever24.tutor.application.resource.ResourceCatalogConflictException;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.PublishStatus;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcResourceCatalogRepositoryTest {

    private JdbcResourceCatalogRepository repository;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:resource-catalog-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ResourceDatabasePopulator schema = new ResourceDatabasePopulator(
                new ClassPathResource("curriculum-schema-h2.sql"),
                new ClassPathResource("resource-catalog-schema-h2.sql"));
        schema.execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        seedSkillUnitVariant();
        repository = new JdbcResourceCatalogRepository(
                jdbcTemplate,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void roundTripsExactVersionAndPublishedCandidateProjection() {
        var entry = ResourceCatalogTestFixture.publishedEntry();
        transactionTemplate.executeWithoutResult(ignored -> assertEquals(
                CatalogWriteOutcome.CREATED,
                repository.saveExactVersion(entry).outcome()));

        var exact = repository.findExactVersion(
                entry.resource().resourceKey(), entry.resourceVersion().semanticVersion()).orElseThrow();
        var candidates = repository.findPublishedCandidates(new ResourceCandidateQuery(
                CefrLevel.B1,
                "travel.confirm_gate_change.b1",
                "Travel",
                "GATE_CHANGE",
                null));

        assertEquals(entry.resourceVersion().manifestHash(), exact.resourceVersion().manifestHash());
        assertEquals(2, exact.assets().size());
        assertEquals(1, candidates.size());
        assertEquals("season1.ep006.gate_change.b1.task-hero", candidates.getFirst().taskHero().assetKey());
        assertTrue(repository.findProvider(entry.provider().providerCode()).isPresent());
        assertTrue(repository.findCollection(entry.collection().collectionKey()).isPresent());
    }

    @Test
    void sameVersionAndHashIsIdempotentButDifferentHashConflicts() {
        transactionTemplate.executeWithoutResult(ignored -> repository.saveExactVersion(
                ResourceCatalogTestFixture.publishedEntry()));

        assertEquals(CatalogWriteOutcome.ALREADY_EXISTS,
                transactionTemplate.execute(ignored -> repository.saveExactVersion(
                        ResourceCatalogTestFixture.publishedEntry())).outcome());
        assertThrows(ResourceCatalogConflictException.class, () -> transactionTemplate.execute(ignored ->
                repository.saveExactVersion(ResourceCatalogTestFixture.entry(
                        "", 'd', PublishStatus.PUBLISHED, CollectionStatus.ACTIVE, AssetStatus.ACTIVE))));
    }

    @Test
    void disabledResourceHistoryRemainsReadableAndIsNotCandidate() {
        var disabled = ResourceCatalogTestFixture.entry(
                ".disabled", 'e', PublishStatus.DISABLED, CollectionStatus.ACTIVE, AssetStatus.ACTIVE);
        transactionTemplate.executeWithoutResult(ignored -> repository.saveExactVersion(disabled));

        assertTrue(repository.findExactVersion(
                disabled.resource().resourceKey(), disabled.resourceVersion().semanticVersion()).isPresent());
        assertTrue(repository.findPublishedCandidates(ResourceCandidateQuery.allPublished()).isEmpty());
    }

    @Test
    void outerTransactionRollsBackWholeCatalogWrite() {
        var replacement = ResourceCatalogTestFixture.entry(
                ".rollback", 'f', PublishStatus.PUBLISHED, CollectionStatus.ACTIVE, AssetStatus.ACTIVE);

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(ignored -> {
            repository.saveExactVersion(replacement);
            throw new IllegalStateException("simulate failure after persistence");
        }));

        assertTrue(repository.findExactVersion(
                replacement.resource().resourceKey(), replacement.resourceVersion().semanticVersion()).isEmpty());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM content_provider WHERE provider_code = ?",
                Integer.class,
                replacement.provider().providerCode()));
    }

    private void seedSkillUnitVariant() {
        jdbcTemplate.update("""
                INSERT INTO curriculum_skill_unit (
                    skill_unit_key, communication_goal, review_template_json, semantic_version,
                    status, created_at_utc, updated_at_utc, version
                ) VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """,
                "travel.confirm_information",
                "Confirm changed travel information",
                "{}",
                "1.0.0");
        Long unitId = jdbcTemplate.queryForObject(
                "SELECT id FROM curriculum_skill_unit WHERE skill_unit_key = ?",
                Long.class,
                "travel.confirm_information");
        jdbcTemplate.update("""
                INSERT INTO curriculum_skill_unit_variant (
                    variant_key, skill_unit_id, cefr_level, communication_complexity,
                    estimated_min_minutes, estimated_max_minutes, training_types_json,
                    scaffolding_levels_json, common_error_tags_json, completion_policy_json,
                    retry_policy_json, mastery_policy_json, status, version
                ) VALUES (?, ?, 'B1', 2, 8, 15, '[]', '[]', '[]', '{}', '{}', '{}', 'ACTIVE', 0)
                """,
                "travel.confirm_gate_change.b1",
                unitId);
    }
}
