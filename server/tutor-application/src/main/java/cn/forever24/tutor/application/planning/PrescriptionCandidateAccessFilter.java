package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.profile.UserKey;

import java.util.List;

@FunctionalInterface
public interface PrescriptionCandidateAccessFilter {

    List<PublishedResourceCandidate> accessibleFor(
            UserKey currentUser,
            List<PublishedResourceCandidate> candidates
    );
}
