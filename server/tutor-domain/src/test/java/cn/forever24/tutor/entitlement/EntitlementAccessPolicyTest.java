package cn.forever24.tutor.entitlement;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitlementAccessPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");
    private static final UserKey LEARNER = new UserKey("usr_learner");
    private final AccessPolicy policy = new AccessPolicy();

    @Test
    void grantRevokeAndExpireAreIdempotent() {
        Entitlement granted = Entitlement.grant(
                "ent_1", LEARNER, "private", 9, NOW, NOW.plusSeconds(60), "verified");

        assertSame(granted, granted.grant(9, NOW.plusSeconds(1), NOW.plusSeconds(60), "verified"));
        Entitlement revoked = granted.revoke(NOW.plusSeconds(2), "manual revoke");
        assertEquals(EntitlementStatus.REVOKED, revoked.status());
        assertSame(revoked, revoked.revoke(NOW.plusSeconds(3), "ignored duplicate"));

        Entitlement expiring = Entitlement.grant(
                "ent_2", LEARNER, "private", 9, NOW, NOW.plusSeconds(10), null);
        assertSame(expiring, expiring.expire(NOW.plusSeconds(9)));
        assertEquals(EntitlementStatus.EXPIRED, expiring.expire(NOW.plusSeconds(10)).status());
    }

    @Test
    void publicResourceIsAllowedWithoutEntitlement() {
        AccessDecision decision = decide(AccessScope.PUBLIC, AccessScope.PUBLIC, Optional.empty(), false);

        assertTrue(decision.allowed());
        assertEquals(AccessDecisionReason.ALLOWED_PUBLIC, decision.reason());
    }

    @Test
    void adminGrantedRequiresOwnedActiveEntitlement() {
        AccessDecision missing = decide(
                AccessScope.ADMIN_GRANTED, AccessScope.ADMIN_GRANTED, Optional.empty(), false);
        Entitlement anotherUsers = Entitlement.grant(
                "ent_other", new UserKey("usr_other"), "private", 9, NOW, null, null);
        AccessDecision mismatch = decide(
                AccessScope.ADMIN_GRANTED, AccessScope.ADMIN_GRANTED, Optional.of(anotherUsers), false);
        Entitlement owned = Entitlement.grant("ent_owned", LEARNER, "private", 9, NOW, null, null);
        AccessDecision allowed = decide(
                AccessScope.ADMIN_GRANTED, AccessScope.ADMIN_GRANTED, Optional.of(owned), false);

        assertEquals(AccessDecisionReason.ENTITLEMENT_REQUIRED, missing.reason());
        assertEquals(AccessDecisionReason.ENTITLEMENT_OWNERSHIP_MISMATCH, mismatch.reason());
        assertEquals(AccessDecisionReason.ALLOWED_ADMIN_GRANTED, allowed.reason());
    }

    @Test
    void expiredAndRevokedEntitlementsHaveStableReasons() {
        Entitlement expired = Entitlement.grant(
                "ent_expired", LEARNER, "private", 9, NOW.minusSeconds(120), NOW.minusSeconds(60), null);
        Entitlement revoked = Entitlement.grant(
                "ent_revoked", LEARNER, "private", 9, NOW.minusSeconds(60), null, null)
                .revoke(NOW.minusSeconds(1), null);

        assertEquals(AccessDecisionReason.ENTITLEMENT_EXPIRED,
                decide(AccessScope.ADMIN_GRANTED, AccessScope.ADMIN_GRANTED, Optional.of(expired), false).reason());
        assertEquals(AccessDecisionReason.ENTITLEMENT_REVOKED,
                decide(AccessScope.ADMIN_GRANTED, AccessScope.ADMIN_GRANTED, Optional.of(revoked), false).reason());
    }

    @Test
    void adminOnlyAndDisabledScopesAreEnforcedBeforeEntitlement() {
        AccessDecision learner = decide(AccessScope.ADMIN_ONLY, AccessScope.PUBLIC, Optional.empty(), false);
        AccessDecision admin = decide(AccessScope.ADMIN_ONLY, AccessScope.PUBLIC, Optional.empty(), true);
        AccessDecision disabled = decide(AccessScope.DISABLED, AccessScope.PUBLIC, Optional.empty(), true);

        assertFalse(learner.allowed());
        assertEquals(AccessDecisionReason.ADMIN_ONLY, learner.reason());
        assertEquals(AccessDecisionReason.ALLOWED_ADMIN_ONLY, admin.reason());
        assertEquals(AccessDecisionReason.ACCESS_SCOPE_DISABLED, disabled.reason());
    }

    @Test
    void publicationAndCollectionStatusAreCheckedFirst() {
        ResourceAccessFixtures fixtures = new ResourceAccessFixtures();
        AccessDecision unpublished = policy.decide(new AccessRequest(
                LEARNER, true,
                fixtures.resource(AccessScope.PUBLIC, PublishStatus.DRAFT),
                fixtures.collection(AccessScope.PUBLIC, CollectionStatus.ACTIVE),
                Optional.empty(), NOW));
        AccessDecision collectionDisabled = policy.decide(new AccessRequest(
                LEARNER, true,
                fixtures.resource(AccessScope.PUBLIC, PublishStatus.PUBLISHED),
                fixtures.collection(AccessScope.PUBLIC, CollectionStatus.DISABLED),
                Optional.empty(), NOW));

        assertEquals(AccessDecisionReason.RESOURCE_NOT_PUBLISHED, unpublished.reason());
        assertEquals(AccessDecisionReason.COLLECTION_DISABLED, collectionDisabled.reason());
    }

    private AccessDecision decide(
            AccessScope resourceScope,
            AccessScope collectionScope,
            Optional<Entitlement> entitlement,
            boolean admin
    ) {
        ResourceAccessFixtures fixtures = new ResourceAccessFixtures();
        return policy.decide(new AccessRequest(
                LEARNER,
                admin,
                fixtures.resource(resourceScope, PublishStatus.PUBLISHED),
                fixtures.collection(collectionScope, CollectionStatus.ACTIVE),
                entitlement,
                NOW));
    }

    private static final class ResourceAccessFixtures {

        LearningResource resource(AccessScope scope, PublishStatus status) {
            return new LearningResource(
                    "resource-1", "internal", "private", ResourceType.SCENARIO_LESSON,
                    "Gate change", null, "en", CefrLevel.B1, "Travel", "GATE_CHANGE",
                    "Confirm a gate change", scope, status,
                    status == PublishStatus.PUBLISHED ? "1.0.0" : null, 10);
        }

        ResourceCollection collection(AccessScope scope, CollectionStatus status) {
            return new ResourceCollection(
                    "private", "internal", "Private", scope, status,
                    null, "INTERNAL", null, "LEARNER", null);
        }
    }
}
