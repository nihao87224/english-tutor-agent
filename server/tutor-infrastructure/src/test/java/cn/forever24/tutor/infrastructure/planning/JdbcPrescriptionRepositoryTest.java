package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CompletionPolicy;
import cn.forever24.tutor.curriculum.ScaffoldingLevel;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.planning.DailyLearningPrescription;
import cn.forever24.tutor.planning.LearnerInputSnapshot;
import cn.forever24.tutor.planning.PrescriptionBlock;
import cn.forever24.tutor.planning.PrescriptionBlockStatus;
import cn.forever24.tutor.planning.PrescriptionGoal;
import cn.forever24.tutor.planning.PrescriptionResourceRef;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.planning.PrescriptionStatus;
import cn.forever24.tutor.planning.PrescriptionTaskHero;
import cn.forever24.tutor.planning.policy.PedagogicalPolicyVersion;
import cn.forever24.tutor.planning.policy.PrescriptionRankingPolicy.BlockType;
import cn.forever24.tutor.profile.UserKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcPrescriptionRepositoryTest {

    private static final UserKey USER = new UserKey("usr-jdbc-prescription");
    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

    private JdbcPrescriptionRepository repository;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:prescription-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("prescription-schema-h2.sql")).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        seedReferences();
        repository = new JdbcPrescriptionRepository(
                jdbcTemplate, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void roundTripsPersistedPrescriptionWithVersionedResourceAndEvidence() {
        DailyLearningPrescription prescription = prescription("prescription-1", 1, null);

        transactions.executeWithoutResult(ignored -> repository.saveInitialIfAbsent(prescription));
        DailyLearningPrescription restored = repository.findActive(USER, prescription.learningDate()).orElseThrow();

        assertEquals(prescription.prescriptionId(), restored.prescriptionId());
        assertEquals("1.0.0", restored.blocks().getFirst().resource().resourceVersion());
        assertEquals(List.of("confirm_information"), restored.blocks().getFirst().expectedEvidence());
        assertEquals("Lin Muen stands at the airport gate.", restored.blocks().getFirst().taskHero().altText());
    }

    @Test
    void transactionRollbackDoesNotLeaveAPartialPlanOrTask() {
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(ignored -> {
            repository.saveInitialIfAbsent(prescription("prescription-rollback", 1, null));
            throw new IllegalStateException("force rollback");
        }));

        assertTrue(repository.findActive(USER, LocalDate.of(2026, 8, 20)).isEmpty());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM learning_task", Integer.class));
    }

    private void seedReferences() {
        jdbcTemplate.update("INSERT INTO app_user (user_key, status) VALUES (?, 'ACTIVE')", USER.value());
        jdbcTemplate.update(
                "INSERT INTO curriculum_skill_unit_variant (variant_key) VALUES ('travel.confirm-information.a2')");
        jdbcTemplate.update("INSERT INTO learning_resource (resource_key) VALUES ('season1.ep006.gate-change.a2')");
        jdbcTemplate.update(
                "INSERT INTO learning_resource_version (resource_id, semantic_version) VALUES (1, '1.0.0')");
        jdbcTemplate.update("INSERT INTO episode_mapping (mapping_key) VALUES ('s01.ep006.gate-change.a2')");
    }

    private static DailyLearningPrescription prescription(String key, long version, String supersedes) {
        PrescriptionSkillState skillState = new PrescriptionSkillState(
                "speaking", new BigDecimal("0.35"), new BigDecimal("0.75"),
                CefrLevel.A2, 3, NOW.minusSeconds(3600));
        PrescriptionBlock block = new PrescriptionBlock(
                "block-" + version,
                1,
                BlockType.OUTPUT,
                "Airport gate confirmation",
                "travel.confirm-information.a2",
                new PrescriptionResourceRef("season1.ep006.gate-change.a2", "1.0.0"),
                "s01.ep006.gate-change.a2",
                "S01",
                "EP006",
                "GATE_CHANGE",
                CefrLevel.A2,
                ScaffoldingLevel.HIGH,
                TrainingType.ROLE_PLAY,
                5,
                List.of("confirm_information"),
                new CompletionPolicy(1, Set.of("confirm_information"), true),
                null,
                Map.of("SKILL_GAP", new BigDecimal("0.65")),
                new PrescriptionTaskHero(
                        "asset-task-hero-airport", "https://cdn.example.invalid/hero.webp", "16:9",
                        new BigDecimal("0.68"), new BigDecimal("0.42"),
                        "Lin Muen stands at the airport gate."),
                PrescriptionBlockStatus.READY);
        return new DailyLearningPrescription(
                key,
                USER,
                LocalDate.of(2026, 8, 20),
                ZoneId.of("Asia/Shanghai"),
                version,
                PrescriptionStatus.ACTIVE,
                new PrescriptionGoal("GENERAL_COMMUNICATION", "综合英语沟通"),
                List.of(block),
                "今天优先训练机场信息确认。",
                List.of("GOAL_MATCH", "SKILL_GAP"),
                PedagogicalPolicyVersion.V2_P0_1,
                new LearnerInputSnapshot(7, 20, "GENERAL", null, List.of(skillState)),
                NOW,
                NOW.plusSeconds(86_400),
                supersedes);
    }
}
