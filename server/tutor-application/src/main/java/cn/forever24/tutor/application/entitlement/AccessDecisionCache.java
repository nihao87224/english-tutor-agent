package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.profile.UserKey;

import java.time.Duration;
import java.util.Optional;

public interface AccessDecisionCache {

    Optional<AccessDecision> find(UserKey userKey, boolean administrator, String resourceKey);

    void put(UserKey userKey, boolean administrator, String resourceKey, AccessDecision decision, Duration ttl);

    void invalidate(UserKey userKey, String collectionKey);
}
