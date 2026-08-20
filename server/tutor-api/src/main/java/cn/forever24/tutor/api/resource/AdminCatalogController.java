package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.application.resource.CatalogQueryApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final CatalogQueryApplicationService applicationService;
    private final CatalogResponseMapper mapper;

    public AdminCatalogController(CatalogQueryApplicationService applicationService, ObjectMapper objectMapper) {
        this.applicationService = applicationService;
        this.mapper = new CatalogResponseMapper(objectMapper);
    }

    @GetMapping("/learning-resources")
    @PreAuthorize("hasAuthority('RESOURCE_READ')")
    public CatalogPageResponse<CatalogResourceSummaryResponse> listResources(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        var page = applicationService.listForAdmin(cursor, limit);
        return new CatalogPageResponse<>(page.items().stream().map(mapper::summary).toList(), page.nextCursor());
    }

    @GetMapping("/learning-resources/{resourceId}")
    @PreAuthorize("hasAuthority('RESOURCE_READ')")
    public AdminResourceDetailResponse resourceDetail(@PathVariable("resourceId") String resourceId) {
        var versions = applicationService.getForAdmin(resourceId);
        return new AdminResourceDetailResponse(
                resourceId,
                versions.getFirst().resource().activeVersion(),
                versions.stream().map(mapper::detail).toList());
    }

    @GetMapping("/collections")
    @PreAuthorize("hasAuthority('COLLECTION_READ')")
    public CatalogPageResponse<AdminCollectionResponse> listCollections(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        var page = applicationService.listCollectionsForAdmin(cursor, limit);
        return new CatalogPageResponse<>(
                page.items().stream().map(AdminCollectionResponse::from).toList(), page.nextCursor());
    }

    @GetMapping("/collections/{collectionId}")
    @PreAuthorize("hasAuthority('COLLECTION_READ')")
    public AdminCollectionResponse collectionDetail(@PathVariable("collectionId") String collectionId) {
        return AdminCollectionResponse.from(applicationService.getCollectionForAdmin(collectionId));
    }
}
