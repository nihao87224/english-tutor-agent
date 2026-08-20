package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.resource.CatalogQueryApplicationService;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.resource.ResourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning-resources")
public class LearningResourceController {

    private final CatalogQueryApplicationService applicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;
    private final CatalogResponseMapper mapper;

    public LearningResourceController(
            CatalogQueryApplicationService applicationService,
            CurrentUserKeyResolver currentUserKeyResolver,
            ObjectMapper objectMapper
    ) {
        this.applicationService = applicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
        this.mapper = new CatalogResponseMapper(objectMapper);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public CatalogPageResponse<CatalogResourceSummaryResponse> list(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "collectionId", required = false) String collectionId,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "scene", required = false) String scene,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "skillId", required = false) String skillId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit,
            Authentication authentication
    ) {
        var page = applicationService.listForLearner(
                currentUser(), isResourceAdmin(authentication), enumValue(ResourceType.class, type, "type"),
                collectionId, topic, scene, enumValue(CefrLevel.class, level, "level"), skillId, cursor, limit);
        return new CatalogPageResponse<>(page.items().stream().map(mapper::summary).toList(), page.nextCursor());
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("isAuthenticated()")
    public CatalogResourceDetailResponse detail(
            @PathVariable("resourceId") String resourceId,
            Authentication authentication
    ) {
        return mapper.detail(applicationService.getActiveForLearner(
                currentUser(), isResourceAdmin(authentication), resourceId));
    }

    @GetMapping("/{resourceId}/versions/{version}")
    @PreAuthorize("isAuthenticated()")
    public CatalogResourceDetailResponse version(
            @PathVariable("resourceId") String resourceId,
            @PathVariable("version") String version,
            Authentication authentication
    ) {
        return mapper.detail(applicationService.getVersionForLearner(
                currentUser(), isResourceAdmin(authentication), resourceId, version));
    }

    @PostMapping("/{resourceId}/media-access")
    @PreAuthorize("isAuthenticated()")
    public MediaAccessResponse mediaAccess(
            @PathVariable("resourceId") String resourceId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody MediaAccessRequest request,
            Authentication authentication
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!java.util.Set.of("DISPLAY", "PLAYBACK").contains(request.purpose())) {
            throw new IllegalArgumentException("purpose must be DISPLAY or PLAYBACK");
        }
        var access = applicationService.createMediaAccess(
                currentUser(), isResourceAdmin(authentication), resourceId, request.assetId(), idempotencyKey);
        return new MediaAccessResponse(
                access.assetKey(), access.url().toString(), access.expiresAt(), access.mimeType(), access.contentHash());
    }

    private UserKey currentUser() {
        return new UserKey(currentUserKeyResolver.resolve());
    }

    private static boolean isResourceAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("RESOURCE_READ"));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " is invalid");
        }
    }
}
