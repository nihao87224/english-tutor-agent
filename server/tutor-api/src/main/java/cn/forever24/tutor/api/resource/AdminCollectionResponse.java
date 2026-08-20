package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.resource.ResourceCollection;

public record AdminCollectionResponse(
        String collectionId,
        String providerCode,
        String title,
        String accessScope,
        String status,
        String sourceUrl,
        String ownershipType,
        String licenseNote,
        String allowedAudience,
        String adminNote
) {
    static AdminCollectionResponse from(ResourceCollection collection) {
        return new AdminCollectionResponse(
                collection.collectionKey(), collection.providerCode(), collection.title(),
                collection.accessScope().name(), collection.status().name(), collection.sourceUrl(),
                collection.ownershipType(), collection.licenseNote(), collection.allowedAudience(),
                collection.adminNote());
    }
}
