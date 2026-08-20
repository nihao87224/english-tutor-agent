package cn.forever24.tutor.application.curriculum;

import cn.forever24.tutor.curriculum.CurriculumCatalog;

public interface CurriculumRepository extends SkillGraphRepository, SkillUnitRepository {

    void replace(CurriculumCatalog catalog);
}
