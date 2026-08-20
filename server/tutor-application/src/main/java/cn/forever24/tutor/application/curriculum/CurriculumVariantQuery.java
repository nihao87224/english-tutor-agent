package cn.forever24.tutor.application.curriculum;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CurriculumStatus;

public record CurriculumVariantQuery(CefrLevel level, String skillKey, CurriculumStatus status) {

    public CurriculumVariantQuery {
        if (skillKey != null) {
            if (skillKey.isBlank()) {
                throw new IllegalArgumentException("skillKey must not be blank");
            }
            skillKey = skillKey.strip();
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
    }

    public static CurriculumVariantQuery active(CefrLevel level, String skillKey) {
        return new CurriculumVariantQuery(level, skillKey, CurriculumStatus.ACTIVE);
    }
}
