package cn.forever24.tutor.application.admin;

import java.time.Instant;

public record AdminSystemSetting(
        String key,
        String value,
        String valueType,
        String description,
        Instant updatedAt
) {
}
