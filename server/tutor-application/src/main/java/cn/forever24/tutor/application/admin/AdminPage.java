package cn.forever24.tutor.application.admin;

import java.util.List;

public record AdminPage<T>(
        List<T> items,
        int page,
        int size,
        long total
) {

    public AdminPage {
        items = List.copyOf(items);
    }
}
