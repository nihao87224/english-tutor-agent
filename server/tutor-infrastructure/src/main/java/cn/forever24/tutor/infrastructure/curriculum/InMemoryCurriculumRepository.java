package cn.forever24.tutor.infrastructure.curriculum;

import cn.forever24.tutor.application.curriculum.CurriculumRepository;
import cn.forever24.tutor.application.curriculum.CurriculumVariantQuery;
import cn.forever24.tutor.curriculum.CurriculumCatalog;
import cn.forever24.tutor.curriculum.CurriculumStatus;
import cn.forever24.tutor.curriculum.SkillNode;
import cn.forever24.tutor.curriculum.SkillUnitVariant;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryCurriculumRepository implements CurriculumRepository {

    private final AtomicReference<CurriculumCatalog> catalog = new AtomicReference<>();

    @Override
    public void replace(CurriculumCatalog replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("catalog is required");
        }
        catalog.set(replacement);
    }

    @Override
    public Optional<SkillNode> findSkill(String skillKey) {
        CurriculumCatalog current = catalog.get();
        if (current == null) {
            return Optional.empty();
        }
        return current.skills().stream().filter(skill -> skill.skillKey().equals(skillKey)).findFirst();
    }

    @Override
    public List<SkillNode> findSkills() {
        CurriculumCatalog current = catalog.get();
        if (current == null) {
            return List.of();
        }
        return current.skills().stream().sorted(Comparator.comparing(SkillNode::skillKey)).toList();
    }

    @Override
    public List<SkillUnitVariant> findVariants(CurriculumVariantQuery query) {
        CurriculumCatalog current = catalog.get();
        if (current == null) {
            return List.of();
        }
        Map<String, SkillNode> skillByKey = current.skills().stream()
                .collect(Collectors.toUnmodifiableMap(SkillNode::skillKey, Function.identity()));
        return current.skillUnits().stream()
                .filter(unit -> unit.status() == CurriculumStatus.ACTIVE)
                .flatMap(unit -> unit.variants().stream())
                .filter(variant -> variant.status() == query.status())
                .filter(variant -> query.level() == null || variant.level() == query.level())
                .filter(variant -> query.skillKey() == null
                        || skillByKey.containsKey(query.skillKey())
                        && skillByKey.get(query.skillKey()).status() == CurriculumStatus.ACTIVE)
                .filter(variant -> query.skillKey() == null
                        || variant.targetSkillKeys().contains(query.skillKey())
                        || variant.supportingSkillKeys().contains(query.skillKey()))
                .filter(variant -> variant.targetSkillKeys().stream()
                        .map(skillByKey::get)
                        .allMatch(skill -> skill != null && skill.status() == CurriculumStatus.ACTIVE))
                .sorted(Comparator.comparing(SkillUnitVariant::variantKey))
                .toList();
    }
}
