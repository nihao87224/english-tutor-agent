package cn.forever24.tutor.application.resource;

import java.util.List;

public record CatalogPage<T>(List<T> items, String nextCursor) {
    public CatalogPage {
        items = List.copyOf(items);
    }
}
