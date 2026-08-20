package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.application.resource.HistoricalResourceAccessRepository;
import cn.forever24.tutor.profile.UserKey;

/**
 * Until V2 lesson sessions persist catalog version references, no learner may request an arbitrary old version.
 */
public final class DenyHistoricalResourceAccessRepository implements HistoricalResourceAccessRepository {

    @Override
    public boolean hasSessionOrEvidenceReference(UserKey userKey, String resourceKey, String semanticVersion) {
        return false;
    }
}
