package cn.forever24.tutor.application.curriculum;

import cn.forever24.tutor.curriculum.SkillUnitVariant;

import java.util.List;

public interface SkillUnitRepository {

    List<SkillUnitVariant> findVariants(CurriculumVariantQuery query);
}
