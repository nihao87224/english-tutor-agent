package cn.forever24.tutor.application.curriculum;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.curriculum.CurriculumCatalog;
import cn.forever24.tutor.curriculum.SkillNode;
import cn.forever24.tutor.curriculum.SkillUnitVariant;

import java.util.List;
import java.util.Optional;

public class CurriculumApplicationService {

    private final CurriculumRepository repository;

    public CurriculumApplicationService(CurriculumRepository repository) {
        this.repository = repository;
    }

    public void importCatalog(CurriculumCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("catalog is required");
        }
        repository.replace(catalog);
    }

    public List<SkillUnitVariant> findActiveVariants(CefrLevel level, String skillKey) {
        return repository.findVariants(CurriculumVariantQuery.active(level, skillKey));
    }

    public Optional<SkillNode> findSkill(String skillKey) {
        if (skillKey == null || skillKey.isBlank()) {
            throw new IllegalArgumentException("skillKey is required");
        }
        return repository.findSkill(skillKey.strip());
    }
}
