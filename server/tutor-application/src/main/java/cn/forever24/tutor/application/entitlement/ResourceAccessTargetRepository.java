package cn.forever24.tutor.application.entitlement;

import java.util.Optional;

public interface ResourceAccessTargetRepository {

    Optional<ResourceAccessTarget> findByResourceKey(String resourceKey);

    boolean collectionExists(String collectionKey);
}
