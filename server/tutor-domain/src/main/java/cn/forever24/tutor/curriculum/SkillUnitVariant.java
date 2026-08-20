package cn.forever24.tutor.curriculum;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SkillUnitVariant(
        String variantKey,
        CefrLevel level,
        int communicationComplexity,
        DurationRange duration,
        Set<TrainingType> trainingTypes,
        Set<ScaffoldingLevel> scaffoldingLevels,
        Set<String> commonErrorTags,
        Set<String> targetSkillKeys,
        Set<String> supportingSkillKeys,
        Set<Prerequisite> prerequisites,
        List<EvidenceCriterion> evidenceCriteria,
        CompletionPolicy completionPolicy,
        RetryPolicy retryPolicy,
        MasteryImpactPolicy masteryImpactPolicy,
        CurriculumStatus status
) {

    public SkillUnitVariant {
        if (variantKey == null || variantKey.isBlank() || variantKey.strip().length() > 192) {
            throw new IllegalArgumentException("valid variantKey is required");
        }
        variantKey = variantKey.strip();
        if (level == null || duration == null || completionPolicy == null || retryPolicy == null
                || masteryImpactPolicy == null || status == null) {
            throw new IllegalArgumentException("variant level, policies, duration and status are required");
        }
        if (communicationComplexity < 1 || communicationComplexity > 5) {
            throw new IllegalArgumentException("communicationComplexity must be between 1 and 5");
        }
        targetSkillKeys = requiredSet(targetSkillKeys, "targetSkillKeys");
        supportingSkillKeys = supportingSkillKeys == null ? Set.of() : Set.copyOf(supportingSkillKeys);
        if (!java.util.Collections.disjoint(targetSkillKeys, supportingSkillKeys)) {
            throw new IllegalArgumentException("target and supporting skills must not overlap");
        }
        trainingTypes = requiredSet(trainingTypes, "trainingTypes");
        scaffoldingLevels = requiredSet(scaffoldingLevels, "scaffoldingLevels");
        commonErrorTags = commonErrorTags == null ? Set.of() : Set.copyOf(commonErrorTags);
        prerequisites = prerequisites == null ? Set.of() : Set.copyOf(prerequisites);
        if (prerequisites.stream().map(Prerequisite::skillKey).distinct().count() != prerequisites.size()) {
            throw new IllegalArgumentException("duplicate prerequisite skill key");
        }
        if (evidenceCriteria == null || evidenceCriteria.isEmpty()) {
            throw new IllegalArgumentException("at least one observable evidence criterion is required");
        }
        if (evidenceCriteria.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("evidenceCriteria must not contain null");
        }
        evidenceCriteria = List.copyOf(evidenceCriteria);
        assertUnique(evidenceCriteria, EvidenceCriterion::criterionKey, "duplicate evidence criterion key");
        Set<String> criterionKeys = evidenceCriteria.stream()
                .map(EvidenceCriterion::criterionKey)
                .collect(Collectors.toUnmodifiableSet());
        if (!criterionKeys.containsAll(completionPolicy.requiredCriterionKeys())) {
            throw new IllegalArgumentException("completion policy references an unknown evidence criterion");
        }
    }

    private static <T> Set<T> requiredSet(Set<T> values, String field) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return Set.copyOf(values);
    }

    private static <T> void assertUnique(List<T> values, Function<T, String> key, String message) {
        if (values.stream().map(key).distinct().count() != values.size()) {
            throw new IllegalArgumentException(message);
        }
    }
}
