package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.profile.UserKey;

public interface HistoricalResourceAccessRepository {

    boolean hasSessionOrEvidenceReference(UserKey userKey, String resourceKey, String semanticVersion);
}
