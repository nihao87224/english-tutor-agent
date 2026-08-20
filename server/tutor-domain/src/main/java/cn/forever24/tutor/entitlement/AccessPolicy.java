package cn.forever24.tutor.entitlement;

import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.PublishStatus;

public final class AccessPolicy {

    public static final String POLICY_VERSION = "access-policy-v1";

    public AccessDecision decide(AccessRequest request) {
        String collectionKey = request.collection().collectionKey();
        var now = request.evaluatedAt();

        if (request.resource().publishStatus() == PublishStatus.DISABLED) {
            return AccessDecision.deny(AccessDecisionReason.RESOURCE_DISABLED, collectionKey, now);
        }
        if (request.resource().publishStatus() != PublishStatus.PUBLISHED) {
            return AccessDecision.deny(AccessDecisionReason.RESOURCE_NOT_PUBLISHED, collectionKey, now);
        }
        if (request.collection().status() == CollectionStatus.DISABLED) {
            return AccessDecision.deny(AccessDecisionReason.COLLECTION_DISABLED, collectionKey, now);
        }
        if (request.resource().accessScope() == AccessScope.DISABLED
                || request.collection().accessScope() == AccessScope.DISABLED) {
            return AccessDecision.deny(AccessDecisionReason.ACCESS_SCOPE_DISABLED, collectionKey, now);
        }
        if (request.resource().accessScope() == AccessScope.ADMIN_ONLY
                || request.collection().accessScope() == AccessScope.ADMIN_ONLY) {
            return request.administrator()
                    ? AccessDecision.allow(AccessDecisionReason.ALLOWED_ADMIN_ONLY, collectionKey, now)
                    : AccessDecision.deny(AccessDecisionReason.ADMIN_ONLY, collectionKey, now);
        }
        if (request.resource().accessScope() == AccessScope.ADMIN_GRANTED
                || request.collection().accessScope() == AccessScope.ADMIN_GRANTED) {
            return decideGrantedAccess(request);
        }
        return AccessDecision.allow(AccessDecisionReason.ALLOWED_PUBLIC, collectionKey, now);
    }

    private static AccessDecision decideGrantedAccess(AccessRequest request) {
        String collectionKey = request.collection().collectionKey();
        var now = request.evaluatedAt();
        if (request.entitlement().isEmpty()) {
            return AccessDecision.deny(AccessDecisionReason.ENTITLEMENT_REQUIRED, collectionKey, now);
        }
        Entitlement entitlement = request.entitlement().orElseThrow();
        if (!entitlement.userKey().equals(request.actor())
                || !entitlement.collectionKey().equals(collectionKey)) {
            return AccessDecision.deny(AccessDecisionReason.ENTITLEMENT_OWNERSHIP_MISMATCH, collectionKey, now);
        }
        if (entitlement.status() == EntitlementStatus.REVOKED) {
            return AccessDecision.deny(AccessDecisionReason.ENTITLEMENT_REVOKED, collectionKey, now);
        }
        if (!entitlement.isActiveAt(now)) {
            return AccessDecision.deny(AccessDecisionReason.ENTITLEMENT_EXPIRED, collectionKey, now);
        }
        return AccessDecision.allow(AccessDecisionReason.ALLOWED_ADMIN_GRANTED, collectionKey, now);
    }
}
