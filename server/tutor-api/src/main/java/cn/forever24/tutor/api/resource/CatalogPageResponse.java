package cn.forever24.tutor.api.resource;

import java.util.List;

public record CatalogPageResponse<T>(List<T> items, String nextCursor) {
    public CatalogPageResponse {
        items = List.copyOf(items);
    }
}
