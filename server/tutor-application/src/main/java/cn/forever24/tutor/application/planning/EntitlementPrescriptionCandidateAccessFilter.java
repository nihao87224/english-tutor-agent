package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.application.entitlement.AccessBeforeRankingFilter;
import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.profile.UserKey;

import java.util.List;
import java.util.Objects;

public final class EntitlementPrescriptionCandidateAccessFilter implements PrescriptionCandidateAccessFilter {

    private final AccessBeforeRankingFilter delegate;

    public EntitlementPrescriptionCandidateAccessFilter(AccessBeforeRankingFilter delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public List<PublishedResourceCandidate> accessibleFor(
            UserKey currentUser,
            List<PublishedResourceCandidate> candidates
    ) {
        return delegate.filterBeforeRanking(currentUser, false, candidates);
    }
}
