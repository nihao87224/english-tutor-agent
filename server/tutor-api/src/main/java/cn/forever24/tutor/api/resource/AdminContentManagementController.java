package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.application.content.ContentImportApplicationService;
import cn.forever24.tutor.application.entitlement.EntitlementAdminActor;
import cn.forever24.tutor.application.entitlement.EntitlementApplicationService;
import cn.forever24.tutor.application.entitlement.GrantEntitlementCommand;
import cn.forever24.tutor.application.entitlement.RevokeEntitlementCommand;
import cn.forever24.tutor.application.resource.CatalogManagementApplicationService;
import cn.forever24.tutor.content.ContentImportBatch;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminContentManagementController {
    private final ContentImportApplicationService imports;
    private final CatalogManagementApplicationService catalog;
    private final EntitlementApplicationService entitlements;
    private final CatalogResponseMapper mapper;

    public AdminContentManagementController(
            ContentImportApplicationService imports,
            CatalogManagementApplicationService catalog,
            EntitlementApplicationService entitlements,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        this.imports = imports;
        this.catalog = catalog;
        this.entitlements = entitlements;
        this.mapper = new CatalogResponseMapper(objectMapper);
    }

    @PostMapping("/content-imports")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ContentImportBatch importManifest(@RequestBody AdminContentImportRequest request) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        return imports.importManifest(request.manifestJson());
    }

    @PostMapping("/learning-resources/{resourceKey}/versions/{semanticVersion}/publish")
    @PreAuthorize("hasAuthority('RESOURCE_PUBLISH')")
    public AdminResourceDetailResponse publish(
            @PathVariable String resourceKey, @PathVariable String semanticVersion,
            @RequestHeader("If-Match") String ifMatch, Authentication authentication
    ) {
        return detail(catalog.publish(actorUserId(authentication), resourceKey, semanticVersion, ifMatch));
    }

    @PostMapping("/learning-resources/{resourceKey}/versions/{semanticVersion}/unpublish")
    @PreAuthorize("hasAuthority('RESOURCE_PUBLISH')")
    public AdminResourceDetailResponse unpublish(
            @PathVariable String resourceKey, @PathVariable String semanticVersion,
            @RequestHeader("If-Match") String ifMatch, Authentication authentication
    ) {
        return detail(catalog.unpublish(actorUserId(authentication), resourceKey, semanticVersion, ifMatch));
    }

    @PostMapping("/learning-resources/{resourceKey}/versions/{semanticVersion}/disable")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public AdminResourceDetailResponse disable(
            @PathVariable String resourceKey, @PathVariable String semanticVersion,
            @RequestHeader("If-Match") String ifMatch, Authentication authentication
    ) {
        return detail(catalog.disable(actorUserId(authentication), resourceKey, semanticVersion, ifMatch));
    }

    @PatchMapping("/collections/{collectionKey}/availability")
    @PreAuthorize("hasAuthority('COLLECTION_MANAGE')")
    public AdminCollectionResponse setCollectionAvailability(
            @PathVariable String collectionKey, @RequestBody AdminCollectionAvailabilityRequest request,
            Authentication authentication
    ) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        return AdminCollectionResponse.from(catalog.setCollectionDisabled(actorUserId(authentication), collectionKey, request.disabled()));
    }

    @PostMapping("/entitlements/grant")
    @PreAuthorize("hasAuthority('ENTITLEMENT_MANAGE')")
    public Object grant(@RequestBody AdminEntitlementRequest request, Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        return entitlements.grant(actor(authentication), new GrantEntitlementCommand(
                new UserKey(request.userKey()), request.collectionKey(), request.expiresAt(), request.reason()));
    }

    @PostMapping("/entitlements/revoke")
    @PreAuthorize("hasAuthority('ENTITLEMENT_MANAGE')")
    public Object revoke(@RequestBody AdminEntitlementRequest request, Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        return entitlements.revoke(actor(authentication), new RevokeEntitlementCommand(
                new UserKey(request.userKey()), request.collectionKey(), request.reason()));
    }

    private AdminResourceDetailResponse detail(cn.forever24.tutor.resource.ResourceCatalogEntry entry) {
        return new AdminResourceDetailResponse(
                entry.resource().resourceKey(), entry.resource().activeVersion(), java.util.List.of(mapper.detail(entry)));
    }

    private static EntitlementAdminActor actor(Authentication authentication) {
        return new EntitlementAdminActor(actorUserId(authentication), Set.of(EntitlementAdminActor.MANAGE_PERMISSION));
    }

    private static long actorUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    public record AdminContentImportRequest(String manifestJson) { }
    public record AdminCollectionAvailabilityRequest(boolean disabled) { }
    public record AdminEntitlementRequest(String userKey, String collectionKey, Instant expiresAt, String reason) { }
}
