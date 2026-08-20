package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.profile.UserKey;

import java.util.List;
import java.util.Objects;

/**
 * The only candidate hand-off to ranking: inaccessible candidates are removed first.
 */
public final class AccessBeforeRankingFilter {

    private final EntitlementApplicationService entitlementService;

    public AccessBeforeRankingFilter(EntitlementApplicationService entitlementService) {
        this.entitlementService = Objects.requireNonNull(entitlementService);
    }

    public List<PublishedResourceCandidate> filterBeforeRanking(
            UserKey currentUser,
            boolean administrator,
            List<PublishedResourceCandidate> candidates
    ) {
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");
        return candidates.stream()
                .filter(candidate -> entitlementService.decide(
                        currentUser,
                        administrator,
                        candidate.resourceKey()).allowed())
                .toList();
    }
}
