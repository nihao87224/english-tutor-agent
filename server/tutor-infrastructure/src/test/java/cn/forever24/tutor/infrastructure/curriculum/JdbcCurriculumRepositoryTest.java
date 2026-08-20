package cn.forever24.tutor.infrastructure.curriculum;

import cn.forever24.tutor.application.curriculum.CurriculumVariantQuery;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CurriculumStatus;
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

class JdbcCurriculumRepositoryTest {

    private JdbcCurriculumRepository repository;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:curriculum-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("curriculum-schema-h2.sql")).execute(dataSource);
        repository = new JdbcCurriculumRepository(
                new JdbcTemplate(dataSource),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void roundTripsSkillGraphAndActiveVariantQuery() {
        transactionTemplate.executeWithoutResult(ignored -> repository.replace(
                CurriculumTestFixture.catalog("", CurriculumStatus.ACTIVE, CurriculumStatus.ACTIVE)));

        var variants = repository.findVariants(CurriculumVariantQuery.active(
                CefrLevel.B1,
                "travel.confirm_information"));

        assertEquals(2, repository.findSkills().size());
        assertEquals("travel.communication", repository.findSkill("travel.communication").orElseThrow().skillKey());
        assertEquals(1, variants.size());
        assertEquals("travel.confirm_gate_change.b1", variants.getFirst().variantKey());
        assertTrue(variants.getFirst().completionPolicy().completionDoesNotImplyMastery());
        assertTrue(variants.getFirst().retryPolicy().retryEvidenceIsIndependent());
    }

    @Test
    void outerTransactionRollsBackWholeCatalogReplacement() {
        transactionTemplate.executeWithoutResult(ignored -> repository.replace(
                CurriculumTestFixture.catalog("", CurriculumStatus.ACTIVE, CurriculumStatus.ACTIVE)));

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(ignored -> {
            repository.replace(CurriculumTestFixture.catalog(".replacement", CurriculumStatus.ACTIVE, CurriculumStatus.ACTIVE));
            throw new IllegalStateException("simulate a failure after persistence");
        }));

        assertTrue(repository.findSkill("travel.communication").isPresent());
        assertTrue(repository.findSkill("travel.communication.replacement").isEmpty());
        assertEquals(1, repository.findVariants(CurriculumVariantQuery.active(CefrLevel.B1, null)).size());
    }

    @Test
    void disabledTargetSkillIsExcludedByJdbcCandidateQuery() {
        transactionTemplate.executeWithoutResult(ignored -> repository.replace(
                CurriculumTestFixture.catalog("", CurriculumStatus.DISABLED, CurriculumStatus.ACTIVE)));

        assertTrue(repository.findVariants(CurriculumVariantQuery.active(CefrLevel.B1, null)).isEmpty());
    }
}
