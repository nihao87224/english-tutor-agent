package cn.forever24.tutor.infrastructure.entitlement;

import cn.forever24.tutor.application.entitlement.EntitlementAdminActor;
import cn.forever24.tutor.application.entitlement.EntitlementApplicationException;
import cn.forever24.tutor.application.entitlement.EntitlementApplicationService;
import cn.forever24.tutor.application.entitlement.GrantEntitlementCommand;
import cn.forever24.tutor.application.entitlement.RevokeEntitlementCommand;
import cn.forever24.tutor.entitlement.AccessPolicy;
import cn.forever24.tutor.entitlement.AccessDecisionReason;
import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.profile.UserKey;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcEntitlementRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");
    private static final UserKey LEARNER = new UserKey("usr_learner");

    private JdbcTemplate jdbcTemplate;
    private JdbcEntitlementRepository repository;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:entitlement-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("curriculum-schema-h2.sql"),
                new ClassPathResource("resource-catalog-schema-h2.sql"),
                new ClassPathResource("entitlement-schema-h2.sql"))
                .execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        repository = new JdbcEntitlementRepository(jdbcTemplate);
        seedAccessTarget();
    }

    @Test
    void roundTripsEntitlementAndAuthoritativeAccessTarget() {
        Entitlement entitlement = Entitlement.grant(
                "ent_1", LEARNER, "private", 9, NOW, NOW.plusSeconds(600), "verified");

        transactionTemplate.executeWithoutResult(ignored -> repository.insert(entitlement));

        assertEquals(entitlement, repository.find(LEARNER, "private").orElseThrow());
        assertEquals("private-resource",
                repository.findByResourceKey("private-resource").orElseThrow().resource().resourceKey());
        assertTrue(repository.collectionExists("private"));
    }

    @Test
    void optimisticUpdateRejectsStaleVersion() {
        Entitlement entitlement = Entitlement.grant(
                "ent_1", LEARNER, "private", 9, NOW, null, null);
        transactionTemplate.executeWithoutResult(ignored -> repository.insert(entitlement));
        Entitlement revoked = entitlement.revoke(NOW.plusSeconds(1), null);
        transactionTemplate.executeWithoutResult(ignored -> repository.update(revoked, 0));

        EntitlementApplicationException conflict = assertThrows(
                EntitlementApplicationException.class,
                () -> transactionTemplate.executeWithoutResult(ignored -> repository.update(revoked, 0)));

        assertEquals("ENTITLEMENT_VERSION_CONFLICT", conflict.code());
    }

    @Test
    void auditFailureRollsBackEntitlementMutation() {
        EntitlementApplicationService service = new EntitlementApplicationService(
                repository,
                repository,
                (actor, action, key, before, after, at) -> {
                    throw new IllegalStateException("audit unavailable");
                },
                new SpringEntitlementTransactionOperations(transactionTemplate),
                new InMemoryAccessDecisionCache(Clock.fixed(NOW, ZoneOffset.UTC)),
                () -> "ent_rollback",
                new AccessPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30));

        assertThrows(IllegalStateException.class, () -> service.grant(
                new EntitlementAdminActor(9, Set.of(EntitlementAdminActor.MANAGE_PERMISSION)),
                new GrantEntitlementCommand(LEARNER, "private", null, null)));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_collection_entitlement", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_log", Integer.class));
    }

    @Test
    void successfulGrantCommitsEntitlementAndAuditTogether() {
        EntitlementApplicationService service = new EntitlementApplicationService(
                repository,
                repository,
                new JdbcEntitlementAuditPort(jdbcTemplate),
                new SpringEntitlementTransactionOperations(transactionTemplate),
                new InMemoryAccessDecisionCache(Clock.fixed(NOW, ZoneOffset.UTC)),
                () -> "ent_committed",
                new AccessPolicy(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30));

        service.grant(
                new EntitlementAdminActor(9, Set.of(EntitlementAdminActor.MANAGE_PERMISSION)),
                new GrantEntitlementCommand(LEARNER, "private", null, "verified"));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_collection_entitlement", Integer.class));
        assertEquals("ENTITLEMENT_GRANTED", jdbcTemplate.queryForObject(
                "SELECT action_code FROM admin_audit_log", String.class));
    }

    @Test
    void authoritativeStartDecisionSerializesBehindConcurrentRevoke() throws Exception {
        Entitlement granted = Entitlement.grant(
                "ent_competing", LEARNER, "private", 9, NOW, null, null);
        transactionTemplate.executeWithoutResult(ignored -> repository.insert(granted));
        CountDownLatch revokeHasLock = new CountDownLatch(1);
        CountDownLatch allowRevokeCommit = new CountDownLatch(1);
        EntitlementApplicationService service = new EntitlementApplicationService(
                repository,
                repository,
                (actor, action, key, before, after, at) -> {
                    if ("ENTITLEMENT_REVOKED".equals(action)) {
                        revokeHasLock.countDown();
                        await(allowRevokeCommit);
                    }
                },
                new SpringEntitlementTransactionOperations(transactionTemplate),
                new InMemoryAccessDecisionCache(Clock.fixed(NOW, ZoneOffset.UTC)),
                () -> "unused",
                new AccessPolicy(),
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC),
                Duration.ofSeconds(30));
        EntitlementAdminActor admin = new EntitlementAdminActor(
                9, Set.of(EntitlementAdminActor.MANAGE_PERMISSION));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var revokeFuture = executor.submit(() -> service.revoke(
                    admin, new RevokeEntitlementCommand(LEARNER, "private", null)));
            assertTrue(revokeHasLock.await(2, TimeUnit.SECONDS));

            var startDecisionFuture = executor.submit(() -> service.decideAuthoritatively(
                    LEARNER, false, "private-resource"));
            assertThrows(TimeoutException.class, () -> startDecisionFuture.get(150, TimeUnit.MILLISECONDS),
                    "start decision must wait for the entitlement row lock");

            allowRevokeCommit.countDown();
            revokeFuture.get(2, TimeUnit.SECONDS);
            var decision = startDecisionFuture.get(2, TimeUnit.SECONDS);
            assertEquals(AccessDecisionReason.ENTITLEMENT_REVOKED, decision.reason());
        } finally {
            allowRevokeCommit.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for concurrent test release");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", exception);
        }
    }

    private void seedAccessTarget() {
        jdbcTemplate.update("INSERT INTO app_user (id, user_key) VALUES (1, 'usr_learner'), (9, 'usr_admin')");
        jdbcTemplate.update("""
                INSERT INTO content_provider (provider_code, display_name, provider_type)
                VALUES ('internal', 'Internal', 'INTERNAL')
                """);
        jdbcTemplate.update("""
                INSERT INTO resource_collection (
                    collection_key, provider_code, title, access_scope, status,
                    ownership_type, allowed_audience, created_at_utc, updated_at_utc, version
                ) VALUES (
                    'private', 'internal', 'Private', 'ADMIN_GRANTED', 'ACTIVE',
                    'INTERNAL', 'LEARNER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
                )
                """);
        Long collectionId = jdbcTemplate.queryForObject(
                "SELECT id FROM resource_collection WHERE collection_key = 'private'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO learning_resource (
                    resource_key, provider_code, collection_id, resource_type, title,
                    language, level, topic, scene, communication_goal, access_scope,
                    publish_status, estimated_minutes, created_at_utc, updated_at_utc, version
                ) VALUES (?, 'internal', ?, 'SCENARIO_LESSON', 'Private lesson',
                    'en', 'B1', 'Travel', 'GATE_CHANGE', 'Confirm information', 'ADMIN_GRANTED',
                    'PUBLISHED', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """, "private-resource", collectionId);
        Long resourceId = jdbcTemplate.queryForObject(
                "SELECT id FROM learning_resource WHERE resource_key = 'private-resource'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO learning_resource_version (
                    resource_id, semantic_version, manifest_hash, manifest_json,
                    learner_fit_json, generation_metadata_json, created_at_utc,
                    published_at_utc, status, version
                ) VALUES (?, '1.0.0', 'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    '{}', '{}', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PUBLISHED', 0)
                """, resourceId);
        Long versionId = jdbcTemplate.queryForObject(
                "SELECT id FROM learning_resource_version WHERE resource_id = ?", Long.class, resourceId);
        jdbcTemplate.update("UPDATE learning_resource SET active_version_id = ? WHERE id = ?", versionId, resourceId);
    }
}
