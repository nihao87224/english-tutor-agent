package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.application.entitlement.EntitlementApplicationService;
import cn.forever24.tutor.application.entitlement.EntitlementApplicationException;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CatalogQueryApplicationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ResourceCatalogRepository catalogRepository;
    private final EntitlementApplicationService entitlementService;
    private final HistoricalResourceAccessRepository historicalAccessRepository;
    private final MediaAccessUrlIssuer mediaAccessUrlIssuer;
    private final Clock clock;
    private final Duration mediaAccessTtl;

    public CatalogQueryApplicationService(
            ResourceCatalogRepository catalogRepository,
            EntitlementApplicationService entitlementService,
            HistoricalResourceAccessRepository historicalAccessRepository,
            MediaAccessUrlIssuer mediaAccessUrlIssuer,
            Clock clock,
            Duration mediaAccessTtl
    ) {
        this.catalogRepository = Objects.requireNonNull(catalogRepository);
        this.entitlementService = Objects.requireNonNull(entitlementService);
        this.historicalAccessRepository = Objects.requireNonNull(historicalAccessRepository);
        this.mediaAccessUrlIssuer = Objects.requireNonNull(mediaAccessUrlIssuer);
        this.clock = Objects.requireNonNull(clock);
        this.mediaAccessTtl = Objects.requireNonNull(mediaAccessTtl);
        if (mediaAccessTtl.isZero() || mediaAccessTtl.isNegative()) {
            throw new IllegalArgumentException("mediaAccessTtl must be positive");
        }
    }

    public CatalogPage<PublishedResourceCandidate> listForLearner(
            UserKey userKey,
            boolean administrator,
            ResourceType type,
            String collectionKey,
            String topic,
            String scene,
            CefrLevel level,
            String skillUnitVariantKey,
            String cursor,
            Integer limit
    ) {
        Objects.requireNonNull(userKey, "userKey must not be null");
        ResourceCandidateQuery query = new ResourceCandidateQuery(
                type, optional(collectionKey), level, optional(skillUnitVariantKey),
                optional(topic), optional(scene), null);
        List<PublishedResourceCandidate> accessible = catalogRepository.findPublishedCandidates(query).stream()
                .filter(candidate -> entitlementService.decide(userKey, administrator, candidate.resourceKey()).allowed())
                .sorted(Comparator.comparing(PublishedResourceCandidate::resourceKey))
                .toList();
        return page(accessible, cursor, limit, PublishedResourceCandidate::resourceKey);
    }

    public ResourceCatalogEntry getActiveForLearner(
            UserKey userKey,
            boolean administrator,
            String resourceKey
    ) {
        Objects.requireNonNull(userKey, "userKey must not be null");
        String normalizedKey = required(resourceKey, "resourceId");
        cn.forever24.tutor.entitlement.AccessDecision decision;
        try {
            decision = entitlementService.decide(userKey, administrator, normalizedKey);
        } catch (EntitlementApplicationException exception) {
            throw CatalogApplicationException.notFound("RESOURCE_NOT_FOUND", "resource was not found");
        }
        if (!decision.allowed()) {
            throw CatalogApplicationException.notFound("RESOURCE_NOT_FOUND", "resource was not found");
        }
        return catalogRepository.findPublishedCandidates(ResourceCandidateQuery.allPublished()).stream()
                .filter(candidate -> candidate.resourceKey().equals(normalizedKey))
                .findFirst()
                .flatMap(candidate -> catalogRepository.findExactVersion(
                        candidate.resourceKey(), candidate.semanticVersion()))
                .orElseThrow(() -> CatalogApplicationException.notFound(
                        "RESOURCE_NOT_FOUND", "resource was not found"));
    }

    public ResourceCatalogEntry getVersionForLearner(
            UserKey userKey,
            boolean administrator,
            String resourceKey,
            String semanticVersion
    ) {
        Objects.requireNonNull(userKey, "userKey must not be null");
        String normalizedKey = required(resourceKey, "resourceId");
        String normalizedVersion = required(semanticVersion, "version");
        if (!administrator && !historicalAccessRepository.hasSessionOrEvidenceReference(
                userKey, normalizedKey, normalizedVersion)) {
            throw CatalogApplicationException.notFound("RESOURCE_VERSION_NOT_FOUND", "resource version was not found");
        }
        ResourceCatalogEntry entry = catalogRepository.findExactVersion(normalizedKey, normalizedVersion)
                .orElseThrow(() -> CatalogApplicationException.notFound(
                        "RESOURCE_VERSION_NOT_FOUND", "resource version was not found"));
        if (!administrator && !entitlementService.decide(userKey, false, normalizedKey).allowed()) {
            throw CatalogApplicationException.notFound("RESOURCE_VERSION_NOT_FOUND", "resource version was not found");
        }
        return entry;
    }

    public CatalogMediaAccess createMediaAccess(
            UserKey userKey,
            boolean administrator,
            String resourceKey,
            String assetKey,
            String idempotencyKey
    ) {
        ResourceCatalogEntry entry = getActiveForLearner(userKey, administrator, resourceKey);
        ResourceAsset asset = entry.assets().stream()
                .filter(candidate -> candidate.assetKey().equals(required(assetKey, "assetId")))
                .findFirst()
                .orElseThrow(() -> CatalogApplicationException.notFound("ASSET_NOT_FOUND", "asset was not found"));
        if (asset.status() != AssetStatus.ACTIVE || asset.accessScope() == AccessScope.DISABLED) {
            throw CatalogApplicationException.notFound("ASSET_NOT_FOUND", "asset was not found");
        }
        if (asset.accessScope() == AccessScope.ADMIN_ONLY && !administrator) {
            throw CatalogApplicationException.notFound("ASSET_NOT_FOUND", "asset was not found");
        }
        if (asset.accessScope() == AccessScope.ADMIN_GRANTED
                && entry.resource().accessScope() == AccessScope.PUBLIC
                && entry.collection().accessScope() == AccessScope.PUBLIC
                && !administrator) {
            throw CatalogApplicationException.notFound("ASSET_NOT_FOUND", "asset was not found");
        }
        MediaAccessGrant grant;
        if (asset.accessScope() == AccessScope.PUBLIC) {
            grant = mediaAccessUrlIssuer.publicUrl(asset);
        } else {
            String normalizedIdempotencyKey = required(idempotencyKey, "Idempotency-Key");
            if (normalizedIdempotencyKey.length() > 128) {
                throw CatalogApplicationException.badRequest(
                        "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must not exceed 128 characters");
            }
            grant = mediaAccessUrlIssuer.issuePrivate(
                    userKey, entry.resource().resourceKey(), asset, normalizedIdempotencyKey,
                    clock.instant().plus(mediaAccessTtl));
        }
        return new CatalogMediaAccess(
                asset.assetKey(), grant.url(), grant.expiresAt(), asset.mimeType(), asset.contentHash());
    }

    public CatalogPage<ResourceCatalogEntry> listForAdmin(String cursor, Integer limit) {
        Map<String, ResourceCatalogEntry> resources = new LinkedHashMap<>();
        catalogRepository.findAllResourceVersions().stream()
                .sorted(Comparator.comparing((ResourceCatalogEntry entry) -> entry.resource().resourceKey())
                        .thenComparing(entry -> entry.resourceVersion().createdAt()).reversed())
                .forEach(entry -> resources.merge(
                        entry.resource().resourceKey(), entry, CatalogQueryApplicationService::preferAdminEntry));
        List<ResourceCatalogEntry> sorted = resources.values().stream()
                .sorted(Comparator.comparing(entry -> entry.resource().resourceKey()))
                .toList();
        return page(sorted, cursor, limit, entry -> entry.resource().resourceKey());
    }

    public List<ResourceCatalogEntry> getForAdmin(String resourceKey) {
        List<ResourceCatalogEntry> versions = catalogRepository.findResourceVersions(
                required(resourceKey, "resourceId"));
        if (versions.isEmpty()) {
            throw CatalogApplicationException.notFound("RESOURCE_NOT_FOUND", "resource was not found");
        }
        return versions.stream()
                .sorted(Comparator.comparing((ResourceCatalogEntry entry) -> entry.resourceVersion().createdAt())
                        .reversed())
                .toList();
    }

    public CatalogPage<ResourceCollection> listCollectionsForAdmin(String cursor, Integer limit) {
        List<ResourceCollection> collections = catalogRepository.findCollections().stream()
                .sorted(Comparator.comparing(ResourceCollection::collectionKey))
                .toList();
        return page(collections, cursor, limit, ResourceCollection::collectionKey);
    }

    public ResourceCollection getCollectionForAdmin(String collectionKey) {
        return catalogRepository.findCollection(required(collectionKey, "collectionId"))
                .orElseThrow(() -> CatalogApplicationException.notFound(
                        "COLLECTION_NOT_FOUND", "collection was not found"));
    }

    private static ResourceCatalogEntry preferAdminEntry(
            ResourceCatalogEntry left,
            ResourceCatalogEntry right
    ) {
        String activeVersion = left.resource().activeVersion();
        if (activeVersion != null) {
            if (right.resourceVersion().semanticVersion().equals(activeVersion)) {
                return right;
            }
            if (left.resourceVersion().semanticVersion().equals(activeVersion)) {
                return left;
            }
        }
        return right.resourceVersion().createdAt().isAfter(left.resourceVersion().createdAt()) ? right : left;
    }

    private static <T> CatalogPage<T> page(
            List<T> filteredItems,
            String cursor,
            Integer requestedLimit,
            java.util.function.Function<T, String> keyExtractor
    ) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw CatalogApplicationException.badRequest("INVALID_LIMIT", "limit must be between 1 and 100");
        }
        String afterKey = decodeCursor(cursor);
        List<T> remaining = filteredItems.stream()
                .filter(item -> afterKey == null || keyExtractor.apply(item).compareTo(afterKey) > 0)
                .toList();
        List<T> pageItems = new ArrayList<>(remaining.subList(0, Math.min(limit, remaining.size())));
        String nextCursor = remaining.size() > limit
                ? encodeCursor(keyExtractor.apply(pageItems.getLast()))
                : null;
        return new CatalogPage<>(pageItems, nextCursor);
    }

    private static String encodeCursor(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return required(decoded, "cursor");
        } catch (IllegalArgumentException exception) {
            throw CatalogApplicationException.badRequest("INVALID_CURSOR", "cursor is invalid");
        }
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw CatalogApplicationException.badRequest(
                    "INVALID_" + field.toUpperCase().replace('-', '_'), field + " is required");
        }
        return value.trim();
    }
}
