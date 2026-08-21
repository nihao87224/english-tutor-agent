package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceVersion;
import cn.forever24.tutor.resource.ResourceVersionStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Administrative state changes for already imported, immutable resource versions. */
public final class CatalogManagementApplicationService {
    private final ResourceCatalogRepository catalog;
    private final CatalogManagementAuditPort audit;
    private final Clock clock;

    public CatalogManagementApplicationService(
            ResourceCatalogRepository catalog,
            CatalogManagementAuditPort audit,
            Clock clock
    ) {
        this.catalog = Objects.requireNonNull(catalog);
        this.audit = Objects.requireNonNull(audit);
        this.clock = Objects.requireNonNull(clock);
    }

    public ResourceCatalogEntry publish(long actorUserId, String resourceKey, String semanticVersion, String ifMatch) {
        return changeResource(actorUserId, resourceKey, semanticVersion, ifMatch, Action.PUBLISH);
    }

    public ResourceCatalogEntry unpublish(long actorUserId, String resourceKey, String semanticVersion, String ifMatch) {
        return changeResource(actorUserId, resourceKey, semanticVersion, ifMatch, Action.UNPUBLISH);
    }

    public ResourceCatalogEntry disable(long actorUserId, String resourceKey, String semanticVersion, String ifMatch) {
        return changeResource(actorUserId, resourceKey, semanticVersion, ifMatch, Action.DISABLE);
    }

    public ResourceCollection setCollectionDisabled(long actorUserId, String collectionKey, boolean disabled) {
        ResourceCollection current = catalog.findCollection(required(collectionKey, "collectionKey"))
                .orElseThrow(() -> CatalogApplicationException.notFound("COLLECTION_NOT_FOUND", "collection was not found"));
        ResourceCollection next = new ResourceCollection(
                current.collectionKey(), current.providerCode(), current.title(),
                disabled ? AccessScope.DISABLED : current.accessScope(),
                disabled ? CollectionStatus.DISABLED : CollectionStatus.ACTIVE,
                current.sourceUrl(), current.ownershipType(), current.licenseNote(),
                current.allowedAudience(), current.adminNote());
        catalog.replaceCollection(next);
        audit.append(requireActor(actorUserId), disabled ? "COLLECTION_DISABLED" : "COLLECTION_ACTIVATED", next.collectionKey(), clock.instant());
        return next;
    }

    private ResourceCatalogEntry changeResource(
            long actorUserId, String resourceKey, String semanticVersion, String ifMatch, Action action
    ) {
        ResourceCatalogEntry current = catalog.findExactVersion(required(resourceKey, "resourceKey"), required(semanticVersion, "semanticVersion"))
                .orElseThrow(() -> CatalogApplicationException.notFound("RESOURCE_VERSION_NOT_FOUND", "resource version was not found"));
        if (!current.resourceVersion().manifestHash().equals(required(ifMatch, "If-Match"))) {
            throw CatalogApplicationException.conflict("RESOURCE_VERSION_CONFLICT", "If-Match does not match the imported manifest hash");
        }
        Instant now = clock.instant();
        ResourceVersionStatus versionStatus = switch (action) {
            case PUBLISH -> ResourceVersionStatus.PUBLISHED;
            case UNPUBLISH -> ResourceVersionStatus.UNPUBLISHED;
            case DISABLE -> ResourceVersionStatus.DISABLED;
        };
        PublishStatus publishStatus = switch (action) {
            case PUBLISH -> PublishStatus.PUBLISHED;
            case UNPUBLISH -> PublishStatus.UNPUBLISHED;
            case DISABLE -> PublishStatus.DISABLED;
        };
        if (action == Action.PUBLISH && current.collection().status() != CollectionStatus.ACTIVE) {
            throw CatalogApplicationException.conflict("COLLECTION_DISABLED", "cannot publish into a disabled collection");
        }
        LearningResource resource = new LearningResource(
                current.resource().resourceKey(), current.resource().providerCode(), current.resource().collectionKey(),
                current.resource().type(), current.resource().title(), current.resource().description(), current.resource().language(),
                current.resource().level(), current.resource().topic(), current.resource().scene(), current.resource().communicationGoal(),
                action == Action.DISABLE ? AccessScope.DISABLED : current.resource().accessScope(), publishStatus,
                action == Action.PUBLISH ? current.resourceVersion().semanticVersion() : null, current.resource().estimatedMinutes());
        ResourceVersion version = new ResourceVersion(
                current.resourceVersion().resourceKey(), current.resourceVersion().semanticVersion(), current.resourceVersion().manifestHash(),
                current.resourceVersion().manifestJson(), current.resourceVersion().learnerFitJson(), current.resourceVersion().generationMetadataJson(),
                current.resourceVersion().skillUnitVariantKeys(), current.resourceVersion().assetReferences(), versionStatus,
                current.resourceVersion().createdAt(), action == Action.PUBLISH ? now : current.resourceVersion().publishedAt());
        ResourceCatalogEntry next = new ResourceCatalogEntry(current.provider(), current.collection(), resource, version, current.assets());
        catalog.replacePublicationState(next);
        String auditAction = switch (action) {
            case PUBLISH -> "RESOURCE_PUBLISHED";
            case UNPUBLISH -> "RESOURCE_UNPUBLISHED";
            case DISABLE -> "RESOURCE_DISABLED";
        };
        audit.append(requireActor(actorUserId), auditAction, resource.resourceKey() + "@" + version.semanticVersion(), now);
        return next;
    }

    private static long requireActor(long actorUserId) {
        if (actorUserId <= 0) throw new IllegalArgumentException("actor user id must be positive");
        return actorUserId;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private enum Action { PUBLISH, UNPUBLISH, DISABLE }
}
