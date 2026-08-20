package cn.forever24.tutor.entitlement;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.LearningResource;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AccessRequest(
        UserKey actor,
        boolean administrator,
        LearningResource resource,
        ResourceCollection collection,
        Optional<Entitlement> entitlement,
        Instant evaluatedAt
) {
    public AccessRequest {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(collection, "collection must not be null");
        entitlement = entitlement == null ? Optional.empty() : entitlement;
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        if (!resource.collectionKey().equals(collection.collectionKey())) {
            throw new IllegalArgumentException("resource and collection do not match");
        }
    }
}
