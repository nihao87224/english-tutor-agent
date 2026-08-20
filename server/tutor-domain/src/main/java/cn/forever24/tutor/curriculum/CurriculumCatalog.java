package cn.forever24.tutor.curriculum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record CurriculumCatalog(List<SkillNode> skills, List<SkillUnit> skillUnits) {

    public CurriculumCatalog {
        if (skills == null || skills.isEmpty()) {
            throw new IllegalArgumentException("curriculum must contain skills");
        }
        if (skillUnits == null || skillUnits.isEmpty()) {
            throw new IllegalArgumentException("curriculum must contain Skill Units");
        }
        skills = List.copyOf(skills);
        skillUnits = List.copyOf(skillUnits);
        validate(skills, skillUnits);
    }

    private static void validate(List<SkillNode> skills, List<SkillUnit> units) {
        Map<String, SkillNode> skillByKey = uniqueSkills(skills);
        assertNoParentCycles(skillByKey);

        Set<String> unitKeys = new HashSet<>();
        Set<String> variantKeys = new HashSet<>();
        for (SkillUnit unit : units) {
            if (!unitKeys.add(unit.skillUnitKey())) {
                throw new IllegalArgumentException("duplicate Skill Unit key: " + unit.skillUnitKey());
            }
            for (SkillUnitVariant variant : unit.variants()) {
                if (!variantKeys.add(variant.variantKey())) {
                    throw new IllegalArgumentException("duplicate variant key: " + variant.variantKey());
                }
                Set<String> referencedSkills = new HashSet<>(variant.targetSkillKeys());
                referencedSkills.addAll(variant.supportingSkillKeys());
                variant.prerequisites().forEach(prerequisite -> referencedSkills.add(prerequisite.skillKey()));
                for (String skillKey : referencedSkills) {
                    if (!skillByKey.containsKey(skillKey)) {
                        throw new IllegalArgumentException(
                                "variant " + variant.variantKey() + " references unknown skill: " + skillKey);
                    }
                }
            }
        }
    }

    private static Map<String, SkillNode> uniqueSkills(List<SkillNode> skills) {
        Map<String, SkillNode> skillByKey = new HashMap<>();
        for (SkillNode skill : skills) {
            if (skillByKey.putIfAbsent(skill.skillKey(), skill) != null) {
                throw new IllegalArgumentException("duplicate skill key: " + skill.skillKey());
            }
            if (skill.parentSkillKey() != null && skills.stream().noneMatch(
                    candidate -> candidate.skillKey().equals(skill.parentSkillKey()))) {
                throw new IllegalArgumentException("unknown parent skill: " + skill.parentSkillKey());
            }
        }
        return Map.copyOf(skillByKey);
    }

    private static void assertNoParentCycles(Map<String, SkillNode> skillByKey) {
        for (SkillNode skill : skillByKey.values()) {
            Set<String> path = new HashSet<>();
            SkillNode current = skill;
            while (current != null) {
                if (!path.add(current.skillKey())) {
                    throw new IllegalArgumentException("skill graph contains a cycle at: " + current.skillKey());
                }
                current = current.parentSkillKey() == null ? null : skillByKey.get(current.parentSkillKey());
            }
        }
    }
}
