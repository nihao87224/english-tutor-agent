package cn.forever24.tutor.resource;

public record ResourceCollection(
        String collectionKey,
        String providerCode,
        String title,
        AccessScope accessScope,
        CollectionStatus status,
        String sourceUrl,
        String ownershipType,
        String licenseNote,
        String allowedAudience,
        String adminNote
) {
    public ResourceCollection {
        collectionKey = ResourceValidation.required(collectionKey, "collectionKey");
        providerCode = ResourceValidation.required(providerCode, "providerCode");
        title = ResourceValidation.required(title, "collection title");
        if (accessScope == null || status == null) {
            throw new IllegalArgumentException("collection access scope and status are required");
        }
        ownershipType = ResourceValidation.required(ownershipType, "ownershipType");
        allowedAudience = ResourceValidation.required(allowedAudience, "allowedAudience");
    }
}
