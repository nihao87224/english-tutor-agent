package cn.forever24.tutor.api.resource;

import java.util.List;

public record AdminResourceDetailResponse(
        String resourceId,
        String activeVersion,
        List<CatalogResourceDetailResponse> versions
) {
    public AdminResourceDetailResponse {
        versions = List.copyOf(versions);
    }
}
