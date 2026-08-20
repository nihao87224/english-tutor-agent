package cn.forever24.tutor.application.resource;

import cn.forever24.tutor.application.entitlement.AccessDecisionCache;
import cn.forever24.tutor.application.entitlement.EntitlementApplicationService;
import cn.forever24.tutor.application.entitlement.EntitlementRepository;
import cn.forever24.tutor.application.entitlement.ResourceAccessTarget;
import cn.forever24.tutor.application.entitlement.ResourceAccessTargetRepository;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.entitlement.AccessDecision;
import cn.forever24.tutor.entitlement.AccessPolicy;
import cn.forever24.tutor.entitlement.Entitlement;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.AccessScope;
import cn.forever24.tutor.resource.AssetGenerationMetadata;
import cn.forever24.tutor.resource.AssetMediaType;
import cn.forever24.tutor.resource.AssetPurpose;
import cn.forever24.tutor.resource.AssetReference;
import cn.forever24.tutor.resource.AssetStatus;
import cn.forever24.tutor.resource.CollectionStatus;
import cn.forever24.tutor.resource.ContentProvider;
import cn.forever24.tutor.resource.ContentProviderType;
import cn.forever24.tutor.resource.DisplaySurface;
import cn.forever24.tutor.resource.FocalPoint;
import cn.forever24.tutor.resource.ImageAssetMetadata;
import cn.forever24.tutor.resource.LearningResource;
import cn.forever24.tutor.resource.PublishStatus;
import cn.forever24.tutor.resource.ResourceAsset;
import cn.forever24.tutor.resource.ResourceCatalogEntry;
import cn.forever24.tutor.resource.ResourceCollection;
import cn.forever24.tutor.resource.ResourceType;
import cn.forever24.tutor.resource.ResourceVersion;
import cn.forever24.tutor.resource.ResourceVersionStatus;
import cn.forever24.tutor.resource.ShotType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogQueryApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");
    private static final UserKey USER = new UserKey("usr_owner");

    @Test
    void filtersAccessBeforePaginationWithoutLeakingPrivateCandidates() {
        Fixture fixture = new Fixture(List.of(
                entry("a-private", "private", AccessScope.ADMIN_GRANTED, PublishStatus.PUBLISHED),
                entry("b-public", "public", AccessScope.PUBLIC, PublishStatus.PUBLISHED),
                entry("c-disabled", "public", AccessScope.PUBLIC, PublishStatus.DISABLED)));

        CatalogPage<PublishedResourceCandidate> page = fixture.service.listForLearner(
                USER, false, null, null, null, null, null, null, null, 1);

        assertEquals(List.of("b-public"), page.items().stream().map(PublishedResourceCandidate::resourceKey).toList());
        assertNull(page.nextCursor(), "denied resources must not create an extra page");
    }

    @Test
    void grantAllowsPrivateResourceAndPrivateMediaRequiresIdempotency() {
        ResourceCatalogEntry privateEntry = entry(
                "a-private", "private", AccessScope.ADMIN_GRANTED, PublishStatus.PUBLISHED);
        Fixture fixture = new Fixture(List.of(privateEntry));
        fixture.entitlements.put("usr_owner/private", Entitlement.grant(
                "ent_1", USER, "private", 9, NOW.minusSeconds(60), null, "verified"));

        CatalogPage<PublishedResourceCandidate> page = fixture.service.listForLearner(
                USER, false, null, null, null, null, null, null, null, null);
        CatalogApplicationException missingKey = assertThrows(
                CatalogApplicationException.class,
                () -> fixture.service.createMediaAccess(
                        USER, false, "a-private", "a-private.hero", null));
        CatalogMediaAccess access = fixture.service.createMediaAccess(
                USER, false, "a-private", "a-private.hero", "idem-12345678");

        assertEquals(1, page.items().size());
        assertEquals("INVALID_IDEMPOTENCY_KEY", missingKey.code());
        assertFalse(access.url().toString().contains("images/private"));
        assertEquals(NOW.plusSeconds(600), access.expiresAt());
    }

    @Test
    void historicalVersionRequiresCurrentLearnerOwnershipWhileAdminMayReadIt() {
        Fixture fixture = new Fixture(List.of(entry(
                "b-public", "public", AccessScope.PUBLIC, PublishStatus.PUBLISHED)));
        fixture.historicalOwner = USER;

        assertEquals("1.0.0", fixture.service.getVersionForLearner(
                USER, false, "b-public", "1.0.0").resourceVersion().semanticVersion());
        CatalogApplicationException denied = assertThrows(
                CatalogApplicationException.class,
                () -> fixture.service.getVersionForLearner(
                        new UserKey("usr_other"), false, "b-public", "1.0.0"));
        assertEquals("RESOURCE_VERSION_NOT_FOUND", denied.code());
        assertEquals("1.0.0", fixture.service.getVersionForLearner(
                new UserKey("usr_admin"), true, "b-public", "1.0.0").resourceVersion().semanticVersion());
    }

    @Test
    void validatesCursorAndLimitBoundaries() {
        Fixture fixture = new Fixture(List.of(entry(
                "b-public", "public", AccessScope.PUBLIC, PublishStatus.PUBLISHED)));

        assertEquals("INVALID_CURSOR", assertThrows(CatalogApplicationException.class,
                () -> fixture.service.listForLearner(
                        USER, false, null, null, null, null, null, null, "%%", 20)).code());
        assertEquals("INVALID_LIMIT", assertThrows(CatalogApplicationException.class,
                () -> fixture.service.listForAdmin(null, 101)).code());
    }

    @Test
    void missingResourceUsesCanonicalCatalogErrorWithoutLeakingEntitlementDetails() {
        Fixture fixture = new Fixture(List.of());

        CatalogApplicationException error = assertThrows(
                CatalogApplicationException.class,
                () -> fixture.service.getActiveForLearner(USER, false, "missing"));

        assertEquals("RESOURCE_NOT_FOUND", error.code());
        assertEquals(404, error.status());
    }

    private static ResourceCatalogEntry entry(
            String resourceKey,
            String collectionKey,
            AccessScope scope,
            PublishStatus publishStatus
    ) {
        ContentProvider provider = new ContentProvider("internal", "Internal", ContentProviderType.INTERNAL);
        ResourceCollection collection = new ResourceCollection(
                collectionKey, "internal", collectionKey, scope, CollectionStatus.ACTIVE,
                null, "OWNED", "internal", "LEARNER", "private admin note");
        LearningResource resource = new LearningResource(
                resourceKey, "internal", collectionKey, ResourceType.SCENARIO_LESSON,
                resourceKey, "Lin Muen airport scenario", "en", CefrLevel.B1,
                "Travel", "GATE_CHANGE", "Confirm a gate change", scope, publishStatus,
                "1.0.0", 10);
        ResourceAsset hero = new ResourceAsset(
                resourceKey + ".hero", "1.0.0", AssetMediaType.IMAGE, AssetPurpose.TASK_HERO,
                "images/private/" + resourceKey + ".webp", "sha256:" + "a".repeat(64),
                "image/webp", 1000, scope,
                new ImageAssetMetadata(
                        "Generate Lin Muen full-body at an airport gate with consistent appearance.",
                        new AssetGenerationMetadata("provider", "model", "2026-08", "1.0.0"),
                        Set.of("lin-muen-main-v1"), "16:9", ShotType.ENVIRONMENTAL_FULL_BODY,
                        Set.of(DisplaySurface.SCENARIO_INTRO, DisplaySurface.SCENARIO_TRAINING),
                        new FocalPoint(0.5, 0.5), "Lin Muen stands at the airport boarding gate.",
                        "GATE_CHANGE", "season1"),
                AssetStatus.ACTIVE, NOW);
        ResourceVersion version = new ResourceVersion(
                resourceKey, "1.0.0", "sha256:" + "b".repeat(64),
                "{\"generationPrompt\":\"secret prompt\",\"objectKey\":\"private key\",\"scene\":\"gate\"}",
                "{\"goalTags\":[\"travel\"]}", "{\"promptVersion\":\"1\"}",
                Set.of("travel.confirm.b1"), List.of(new AssetReference(hero.assetKey(), 0)),
                ResourceVersionStatus.PUBLISHED, NOW, NOW);
        return new ResourceCatalogEntry(provider, collection, resource, version, List.of(hero));
    }

    private static final class Fixture {
        private final Map<String, Entitlement> entitlements = new LinkedHashMap<>();
        private UserKey historicalOwner;
        private final CatalogQueryApplicationService service;

        private Fixture(List<ResourceCatalogEntry> entries) {
            FakeCatalogRepository catalog = new FakeCatalogRepository(entries);
            EntitlementStore store = new EntitlementStore(entries, entitlements);
            EntitlementApplicationService entitlementService = new EntitlementApplicationService(
                    store, store, (actor, action, key, before, after, at) -> { },
                    new cn.forever24.tutor.application.entitlement.EntitlementTransactionOperations() {
                        @Override
                        public <T> T execute(Supplier<T> action) {
                            return action.get();
                        }
                    },
                    new NoCache(), () -> "ent_generated", new AccessPolicy(),
                    Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30));
            MediaAccessUrlIssuer issuer = new MediaAccessUrlIssuer() {
                @Override
                public MediaAccessGrant publicUrl(ResourceAsset asset) {
                    return new MediaAccessGrant(URI.create("https://cdn.example/" + asset.assetKey()), null);
                }

                @Override
                public MediaAccessGrant issuePrivate(
                        UserKey userKey, String resourceKey, ResourceAsset asset,
                        String idempotencyKey, Instant expiresAt
                ) {
                    return new MediaAccessGrant(
                            URI.create("https://media.example/access/" + asset.assetKey()), expiresAt);
                }
            };
            service = new CatalogQueryApplicationService(
                    catalog, entitlementService,
                    (userKey, resourceKey, version) -> userKey.equals(historicalOwner),
                    issuer, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(10));
        }
    }

    private static final class FakeCatalogRepository implements ResourceCatalogRepository {
        private final List<ResourceCatalogEntry> entries;

        private FakeCatalogRepository(List<ResourceCatalogEntry> entries) {
            this.entries = List.copyOf(entries);
        }

        @Override
        public ResourceVersionSaveResult saveExactVersion(ResourceCatalogEntry entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ResourceCatalogEntry> findExactVersion(String resourceKey, String semanticVersion) {
            return entries.stream().filter(entry -> entry.resource().resourceKey().equals(resourceKey)
                    && entry.resourceVersion().semanticVersion().equals(semanticVersion)).findFirst();
        }

        @Override
        public List<PublishedResourceCandidate> findPublishedCandidates(ResourceCandidateQuery query) {
            return entries.stream()
                    .filter(entry -> entry.resource().publishStatus() == PublishStatus.PUBLISHED)
                    .filter(entry -> entry.resource().accessScope() != AccessScope.DISABLED)
                    .filter(entry -> query.resourceType() == null || query.resourceType() == entry.resource().type())
                    .filter(entry -> query.collectionKey() == null
                            || query.collectionKey().equals(entry.resource().collectionKey()))
                    .map(entry -> new PublishedResourceCandidate(
                            entry.resource().resourceKey(), entry.resourceVersion().semanticVersion(),
                            entry.resource().providerCode(), entry.resource().collectionKey(),
                            entry.resource().type(), entry.resource().title(), entry.resource().level(),
                            entry.resource().topic(), entry.resource().scene(), entry.resource().communicationGoal(),
                            entry.resource().accessScope(), entry.resource().estimatedMinutes(),
                            entry.resourceVersion().skillUnitVariantKeys(), entry.assets().getFirst(), entry.assets()))
                    .toList();
        }

        @Override public List<ResourceCatalogEntry> findAllResourceVersions() { return entries; }
        @Override public List<ResourceCatalogEntry> findResourceVersions(String key) {
            return entries.stream().filter(entry -> entry.resource().resourceKey().equals(key)).toList();
        }
        @Override public List<ResourceCollection> findCollections() {
            return entries.stream().map(ResourceCatalogEntry::collection).distinct().toList();
        }
        @Override public Optional<ContentProvider> findProvider(String code) { return Optional.empty(); }
        @Override public Optional<ResourceCollection> findCollection(String key) {
            return entries.stream().map(ResourceCatalogEntry::collection)
                    .filter(collection -> collection.collectionKey().equals(key)).findFirst();
        }
        @Override public Optional<ResourceAsset> findAsset(String key) { return Optional.empty(); }
    }

    private static final class EntitlementStore implements EntitlementRepository, ResourceAccessTargetRepository {
        private final List<ResourceCatalogEntry> entries;
        private final Map<String, Entitlement> entitlements;

        private EntitlementStore(List<ResourceCatalogEntry> entries, Map<String, Entitlement> entitlements) {
            this.entries = entries;
            this.entitlements = entitlements;
        }

        @Override public Optional<Entitlement> find(UserKey user, String collection) {
            return Optional.ofNullable(entitlements.get(user.value() + "/" + collection));
        }
        @Override public Optional<Entitlement> findForUpdate(UserKey user, String collection) {
            return find(user, collection);
        }
        @Override public List<Entitlement> findForUser(UserKey user) {
            return entitlements.values().stream().filter(item -> item.userKey().equals(user)).toList();
        }
        @Override public void insert(Entitlement entitlement) {
            entitlements.put(entitlement.userKey().value() + "/" + entitlement.collectionKey(), entitlement);
        }
        @Override public void update(Entitlement entitlement, long version) { insert(entitlement); }
        @Override public Optional<ResourceAccessTarget> findByResourceKey(String key) {
            return entries.stream().filter(entry -> entry.resource().resourceKey().equals(key))
                    .map(entry -> new ResourceAccessTarget(entry.resource(), entry.collection())).findFirst();
        }
        @Override public boolean collectionExists(String key) {
            return entries.stream().anyMatch(entry -> entry.collection().collectionKey().equals(key));
        }
    }

    private static final class NoCache implements AccessDecisionCache {
        @Override public Optional<AccessDecision> find(UserKey user, boolean admin, String resource) {
            return Optional.empty();
        }
        @Override public void put(UserKey user, boolean admin, String resource, AccessDecision decision, Duration ttl) { }
        @Override public void invalidate(UserKey user, String collection) { }
    }
}
