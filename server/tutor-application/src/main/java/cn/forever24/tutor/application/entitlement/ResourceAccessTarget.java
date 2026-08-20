package cn.forever24.tutor.application.entitlement;

import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.ResourceCollection;

import java.util.Objects;

public record ResourceAccessTarget(LearningResource resource, ResourceCollection collection) {
    public ResourceAccessTarget {
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(collection, "collection must not be null");
        if (!resource.collectionKey().equals(collection.collectionKey())) {
            throw new IllegalArgumentException("resource and collection do not match");
        }
    }
}
