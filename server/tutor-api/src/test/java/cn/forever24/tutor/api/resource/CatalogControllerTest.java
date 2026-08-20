package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.resource.CatalogApplicationException;
import cn.forever24.tutor.application.resource.CatalogMediaAccess;
import cn.forever24.tutor.application.resource.CatalogPage;
import cn.forever24.tutor.application.resource.CatalogQueryApplicationService;
import cn.forever24.tutor.application.resource.PublishedResourceCandidate;
import cn.forever24.tutor.curriculum.CefrLevel;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T00:00:00Z");

    @Test
    void learnerListReturnsOnlyApplicationFilteredPage() {
        CatalogQueryApplicationService service = mock(CatalogQueryApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr_1");
        var candidate = candidate(entry());
        when(service.listForLearner(any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogPage<>(List.of(candidate), null));
        LearningResourceController controller = new LearningResourceController(service, resolver, new ObjectMapper());

        var response = controller.list(
                null, null, null, null, null, null, null, 20,
                new TestingAuthenticationToken("1", null, "ROLE_USER"));

        assertEquals(List.of("resource-1"), response.items().stream()
                .map(CatalogResourceSummaryResponse::resourceId).toList());
        verify(service).listForLearner(
                new UserKey("usr_1"), false, null, null, null, null, null, null, null, 20);
    }

    @Test
    void detailRemovesPromptAndPrivateObjectKeyRecursively() throws Exception {
        CatalogQueryApplicationService service = mock(CatalogQueryApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr_1");
        when(service.getActiveForLearner(any(), anyBoolean(), anyString())).thenReturn(entry());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LearningResourceController controller = new LearningResourceController(service, resolver, objectMapper);

        CatalogResourceDetailResponse detail = controller.detail(
                "resource-1", new TestingAuthenticationToken("1", null, "ROLE_USER"));
        String json = objectMapper.writeValueAsString(detail.content()) + detail.assets();

        assertTrue(json.contains("Lin Muen stands at an airport gate"));
        assertFalse(json.contains("generationPrompt"));
        assertFalse(json.contains("secret visual prompt"));
        assertFalse(json.contains("images/private/internal-object.webp"));
        assertFalse(json.contains("objectKey"));
    }

    @Test
    void mediaAccessMapsSafeGrantAndValidatesPurpose() {
        CatalogQueryApplicationService service = mock(CatalogQueryApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr_1");
        when(service.createMediaAccess(any(), anyBoolean(), anyString(), anyString(), anyString()))
                .thenReturn(new CatalogMediaAccess(
                        "asset-1", java.net.URI.create("https://media.example/access/asset-1"),
                        NOW.plusSeconds(600), "image/webp", "sha256:" + "a".repeat(64)));
        LearningResourceController controller = new LearningResourceController(service, resolver, new ObjectMapper());

        var response = controller.mediaAccess(
                "resource-1", "idem-12345678", new MediaAccessRequest("asset-1", "PLAYBACK"),
                new TestingAuthenticationToken("1", null, "ROLE_USER"));

        assertEquals("https://media.example/access/asset-1", response.url());
    }

    @Test
    void adminReadsRequireFrozenCatalogPermissionsAndProblemsUseRfcShape() throws Exception {
        assertEquals("hasAuthority('RESOURCE_READ')", AdminCatalogController.class
                .getMethod("listResources", String.class, Integer.class)
                .getAnnotation(PreAuthorize.class).value());
        assertEquals("hasAuthority('COLLECTION_READ')", AdminCatalogController.class
                .getMethod("listCollections", String.class, Integer.class)
                .getAnnotation(PreAuthorize.class).value());

        CatalogExceptionHandler handler = new CatalogExceptionHandler();
        ResponseEntity<?> response = handler.handleCatalog(
                CatalogApplicationException.notFound("RESOURCE_NOT_FOUND", "resource was not found"));
        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("RESOURCE_NOT_FOUND"));
    }

    private static PublishedResourceCandidate candidate(ResourceCatalogEntry entry) {
        return new PublishedResourceCandidate(
                entry.resource().resourceKey(), entry.resourceVersion().semanticVersion(),
                entry.resource().providerCode(), entry.resource().collectionKey(), entry.resource().type(),
                entry.resource().title(), entry.resource().level(), entry.resource().topic(),
                entry.resource().scene(), entry.resource().communicationGoal(), entry.resource().accessScope(),
                entry.resource().estimatedMinutes(), entry.resourceVersion().skillUnitVariantKeys(),
                entry.assets().getFirst(), entry.assets());
    }

    private static ResourceCatalogEntry entry() {
        ContentProvider provider = new ContentProvider("internal", "Internal", ContentProviderType.INTERNAL);
        ResourceCollection collection = new ResourceCollection(
                "public", "internal", "Public", AccessScope.PUBLIC, CollectionStatus.ACTIVE,
                null, "OWNED", "internal", "LEARNER", "admin only note");
        LearningResource resource = new LearningResource(
                "resource-1", "internal", "public", ResourceType.SCENARIO_LESSON,
                "Gate change", "Help Lin Muen", "en", CefrLevel.B1,
                "Travel", "GATE_CHANGE", "Confirm information", AccessScope.PUBLIC,
                PublishStatus.PUBLISHED, "1.0.0", 10);
        ResourceAsset hero = new ResourceAsset(
                "asset-1", "1.0.0", AssetMediaType.IMAGE, AssetPurpose.TASK_HERO,
                "images/private/internal-object.webp", "sha256:" + "a".repeat(64), "image/webp", 100,
                AccessScope.PUBLIC,
                new ImageAssetMetadata(
                        "Secret visual prompt for Lin Muen that must never be returned by the API.",
                        new AssetGenerationMetadata("provider", "model", "2026-08", "1.0.0"),
                        Set.of("lin-muen-main-v1"), "16:9", ShotType.ENVIRONMENTAL_FULL_BODY,
                        Set.of(DisplaySurface.SCENARIO_INTRO, DisplaySurface.SCENARIO_TRAINING),
                        new FocalPoint(0.5, 0.5), "Lin Muen stands at an airport gate and checks the board.",
                        "GATE_CHANGE", "season1"),
                AssetStatus.ACTIVE, NOW);
        ResourceVersion version = new ResourceVersion(
                "resource-1", "1.0.0", "sha256:" + "b".repeat(64),
                "{\"scene\":\"gate\",\"generationPrompt\":\"secret visual prompt\",\"nested\":{\"objectKey\":\"images/private/internal-object.webp\"}}",
                "{\"goalTags\":[\"travel\"]}", "{\"promptVersion\":\"1\"}",
                Set.of("travel.confirm.b1"), List.of(new AssetReference("asset-1", 0)),
                ResourceVersionStatus.PUBLISHED, NOW, NOW);
        return new ResourceCatalogEntry(provider, collection, resource, version, List.of(hero));
    }
}
