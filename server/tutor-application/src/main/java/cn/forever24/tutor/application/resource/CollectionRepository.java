package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.ResourceCollection;

import java.util.Optional;

public interface CollectionRepository {

    Optional<ResourceCollection> findCollection(String collectionKey);
}
