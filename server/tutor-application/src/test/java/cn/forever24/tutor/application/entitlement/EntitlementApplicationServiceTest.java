package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessDecisionReason;
import cn.forever24.tutor.entitlement.AccessPolicy;
import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitlementApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");
    private static final UserKey LEARNER = new UserKey("usr_learner");
    private static final EntitlementAdminActor ADMIN = new EntitlementAdminActor(
            9, Set.of(EntitlementAdminActor.MANAGE_PERMISSION));

    @Test
    void grantAndRevokeAreIdempotentAndAuditedOnlyWhenStateChanges() {
        Fixture fixture = new Fixture(new RecordingCache());
        GrantEntitlementCommand grant = new GrantEntitlementCommand(
                LEARNER, "private", NOW.plusSeconds(600), "verified");

        assertEquals(EntitlementMutationOutcome.CREATED, fixture.service.grant(ADMIN, grant).outcome());
        assertEquals(EntitlementMutationOutcome.UNCHANGED, fixture.service.grant(ADMIN, grant).outcome());
        RevokeEntitlementCommand revoke = new RevokeEntitlementCommand(LEARNER, "private", "refund");
        assertEquals(EntitlementMutationOutcome.UPDATED, fixture.service.revoke(ADMIN, revoke).outcome());
        assertEquals(EntitlementMutationOutcome.UNCHANGED, fixture.service.revoke(ADMIN, revoke).outcome());

        assertEquals(List.of("ENTITLEMENT_GRANTED", "ENTITLEMENT_REVOKED"), fixture.auditActions);
        assertEquals(0, fixture.service.listForCurrentUser(LEARNER).size());
    }

    @Test
    void managementRequiresPermissionAndExistingCollection() {
        Fixture fixture = new Fixture(new RecordingCache());
        EntitlementAdminActor unauthorized = new EntitlementAdminActor(8, Set.of("ENTITLEMENT_READ"));

        EntitlementApplicationException forbidden = assertThrows(
                EntitlementApplicationException.class,
                () -> fixture.service.grant(unauthorized,
                        new GrantEntitlementCommand(LEARNER, "private", null, null)));
        EntitlementApplicationException notFound = assertThrows(
                EntitlementApplicationException.class,
                () -> fixture.service.grant(ADMIN,
                        new GrantEntitlementCommand(LEARNER, "missing", null, null)));

        assertEquals("ENTITLEMENT_PERMISSION_REQUIRED", forbidden.code());
        assertEquals("COLLECTION_NOT_FOUND", notFound.code());
    }

    @Test
    void expireIsIdempotentAndAuditedOnce() {
        Fixture fixture = new Fixture(new RecordingCache());
        Entitlement due = Entitlement.grant(
                "ent_due",
                LEARNER,
                "private",
                ADMIN.userId(),
                NOW.minusSeconds(120),
                NOW.minusSeconds(1),
                "temporary access");
        fixture.entitlements.put(key(LEARNER, "private"), due);

        assertEquals(EntitlementMutationOutcome.UPDATED,
                fixture.service.expire(ADMIN, LEARNER, "private").outcome());
        assertEquals(EntitlementMutationOutcome.UNCHANGED,
                fixture.service.expire(ADMIN, LEARNER, "private").outcome());

        assertEquals(List.of("ENTITLEMENT_EXPIRED"), fixture.auditActions);
    }

    @Test
    void redisFailureFallsBackToAuthoritativeRepository() {
        Fixture fixture = new Fixture(new FailingCache());
        fixture.service.grant(ADMIN, new GrantEntitlementCommand(LEARNER, "private", null, null));

        AccessDecision decision = fixture.service.decide(LEARNER, false, "private-resource");

        assertTrue(decision.allowed());
        assertEquals(AccessDecisionReason.ALLOWED_ADMIN_GRANTED, decision.reason());
        assertTrue(fixture.repositoryFindCount > 0);
    }

    @Test
    void authoritativeStartDecisionIgnoresStaleCachedAllowAfterRevoke() {
        NonInvalidatingCache cache = new NonInvalidatingCache();
        Fixture fixture = new Fixture(cache);
        fixture.service.grant(ADMIN, new GrantEntitlementCommand(LEARNER, "private", null, null));
        assertTrue(fixture.service.decide(LEARNER, false, "private-resource").allowed());

        fixture.service.revoke(ADMIN, new RevokeEntitlementCommand(LEARNER, "private", null));
        assertTrue(cache.find(LEARNER, false, "private-resource").orElseThrow().allowed());
        int lockedReadsBeforeStart = fixture.repositoryLockedFindCount;
        AccessDecision freshStartDecision = fixture.service.decideAuthoritatively(
                LEARNER, false, "private-resource");

        assertFalse(freshStartDecision.allowed());
        assertEquals(AccessDecisionReason.ENTITLEMENT_REVOKED, freshStartDecision.reason());
        assertEquals(lockedReadsBeforeStart + 1, fixture.repositoryLockedFindCount);
    }

    @Test
    void accessFilterRemovesDeniedCandidatesBeforeRankingReceivesThem() {
        Fixture fixture = new Fixture(new RecordingCache());
        AccessBeforeRankingFilter filter = new AccessBeforeRankingFilter(fixture.service);
        List<PublishedResourceCandidate> candidates = List.of(
                candidate("private-resource", "private"),
                candidate("public-resource", "public"));

        List<PublishedResourceCandidate> filtered = filter.filterBeforeRanking(LEARNER, false, candidates);
        List<String> rankedKeys = filtered.stream()
                .peek(candidate -> assertTrue(candidate.resourceKey().startsWith("public"),
                        "ranking must never receive an inaccessible candidate"))
                .map(PublishedResourceCandidate::resourceKey)
                .toList();

        assertEquals(List.of("public-resource"), rankedKeys);
    }

    @Test
    void learnerCanOnlyListOwnActiveEntitlements() {
        Fixture fixture = new Fixture(new RecordingCache());
        fixture.service.grant(ADMIN, new GrantEntitlementCommand(LEARNER, "private", null, null));
        fixture.service.grant(ADMIN, new GrantEntitlementCommand(
                new UserKey("usr_other"), "private", null, null));

        List<Entitlement> owned = fixture.service.listForCurrentUser(LEARNER);

        assertEquals(1, owned.size());
        assertEquals(LEARNER, owned.getFirst().userKey());
    }

    private static PublishedResourceCandidate candidate(String resourceKey, String collectionKey) {
        return new PublishedResourceCandidate(
                resourceKey, "1.0.0", "internal", collectionKey,
                ResourceType.SCENARIO_LESSON, resourceKey, CefrLevel.B1,
                "Travel", "GATE_CHANGE", "Confirm information",
                "public".equals(collectionKey) ? AccessScope.PUBLIC : AccessScope.ADMIN_GRANTED,
                10, Set.of(), null, List.of());
    }

    private static final class Fixture {
        private final Map<String, Entitlement> entitlements = new LinkedHashMap<>();
        private final Map<String, ResourceAccessTarget> targets = new LinkedHashMap<>();
        private final List<String> auditActions = new ArrayList<>();
        private int repositoryFindCount;
        private int repositoryLockedFindCount;
        private final EntitlementApplicationService service;

        Fixture(AccessDecisionCache cache) {
            register("private-resource", "private", AccessScope.ADMIN_GRANTED);
            register("public-resource", "public", AccessScope.PUBLIC);
            EntitlementRepository repository = new EntitlementRepository() {
                @Override
                public Optional<Entitlement> find(UserKey userKey, String collectionKey) {
                    repositoryFindCount++;
                    return Optional.ofNullable(entitlements.get(key(userKey, collectionKey)));
                }

                @Override
                public Optional<Entitlement> findForUpdate(UserKey userKey, String collectionKey) {
                    repositoryLockedFindCount++;
                    return Optional.ofNullable(entitlements.get(key(userKey, collectionKey)));
                }

                @Override
                public List<Entitlement> findForUser(UserKey userKey) {
                    return entitlements.values().stream()
                            .filter(entitlement -> entitlement.userKey().equals(userKey))
                            .toList();
                }

                @Override
                public void insert(Entitlement entitlement) {
                    entitlements.put(key(entitlement.userKey(), entitlement.collectionKey()), entitlement);
                }

                @Override
                public void update(Entitlement entitlement, long expectedVersion) {
                    Entitlement current = entitlements.get(key(
                            entitlement.userKey(), entitlement.collectionKey()));
                    if (current == null || current.version() != expectedVersion) {
                        throw EntitlementApplicationException.conflict(
                                "ENTITLEMENT_VERSION_CONFLICT", "version changed");
                    }
                    entitlements.put(key(entitlement.userKey(), entitlement.collectionKey()), entitlement);
                }
            };
            ResourceAccessTargetRepository targetRepository = new ResourceAccessTargetRepository() {
                @Override
                public Optional<ResourceAccessTarget> findByResourceKey(String resourceKey) {
                    return Optional.ofNullable(targets.get(resourceKey));
                }

                @Override
                public boolean collectionExists(String collectionKey) {
                    return targets.values().stream().anyMatch(
                            target -> target.collection().collectionKey().equals(collectionKey));
                }
            };
            service = new EntitlementApplicationService(
                    repository,
                    targetRepository,
                    (actor, action, key, before, after, at) -> auditActions.add(action),
                    new DirectTransactions(),
                    cache,
                    () -> "ent_" + (entitlements.size() + 1),
                    new AccessPolicy(),
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    Duration.ofSeconds(30));
        }

        private void register(String resourceKey, String collectionKey, AccessScope scope) {
            ResourceCollection collection = new ResourceCollection(
                    collectionKey, "internal", collectionKey, scope, CollectionStatus.ACTIVE,
                    null, "INTERNAL", null, "LEARNER", null);
            LearningResource resource = new LearningResource(
                    resourceKey, "internal", collectionKey, ResourceType.SCENARIO_LESSON,
                    resourceKey, null, "en", CefrLevel.B1, "Travel", "GATE_CHANGE",
                    "Confirm information", scope, PublishStatus.PUBLISHED, "1.0.0", 10);
            targets.put(resourceKey, new ResourceAccessTarget(resource, collection));
        }
    }

    private static class RecordingCache implements AccessDecisionCache {
        protected final Map<String, AccessDecision> decisions = new LinkedHashMap<>();

        @Override
        public Optional<AccessDecision> find(UserKey userKey, boolean administrator, String resourceKey) {
            return Optional.ofNullable(decisions.get(cacheKey(userKey, administrator, resourceKey)));
        }

        @Override
        public void put(
                UserKey userKey,
                boolean administrator,
                String resourceKey,
                AccessDecision decision,
                Duration ttl
        ) {
            decisions.put(cacheKey(userKey, administrator, resourceKey), decision);
        }

        @Override
        public void invalidate(UserKey userKey, String collectionKey) {
            decisions.entrySet().removeIf(entry -> entry.getKey().startsWith(userKey.value() + ":")
                    && entry.getValue().collectionKey().equals(collectionKey));
        }
    }

    private static final class NonInvalidatingCache extends RecordingCache {
        @Override
        public void invalidate(UserKey userKey, String collectionKey) {
            // Simulates an invalidation race/failure leaving an old allow decision in Redis.
        }
    }

    private static final class FailingCache implements AccessDecisionCache {
        @Override
        public Optional<AccessDecision> find(UserKey userKey, boolean administrator, String resourceKey) {
            throw new IllegalStateException("redis unavailable");
        }

        @Override
        public void put(UserKey userKey, boolean administrator, String resourceKey, AccessDecision decision, Duration ttl) {
            throw new IllegalStateException("redis unavailable");
        }

        @Override
        public void invalidate(UserKey userKey, String collectionKey) {
            throw new IllegalStateException("redis unavailable");
        }
    }

    private static final class DirectTransactions implements EntitlementTransactionOperations {
        @Override
        public <T> T execute(java.util.function.Supplier<T> action) {
            return action.get();
        }
    }

    private static String key(UserKey userKey, String collectionKey) {
        return userKey.value() + ':' + collectionKey;
    }

    private static String cacheKey(UserKey userKey, boolean administrator, String resourceKey) {
        return userKey.value() + ':' + administrator + ':' + resourceKey;
    }
}
