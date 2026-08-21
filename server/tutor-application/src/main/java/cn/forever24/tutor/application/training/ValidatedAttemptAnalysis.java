package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.AttemptAnalysis;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Business validation firewall between provider output and any learning-state mutation. */
public record ValidatedAttemptAnalysis(AttemptAnalysis analysis, int failedCriteria) {
    public static ValidatedAttemptAnalysis from(SpeakingAttemptAnalysisContext context, AttemptAnalysis analysis) {
        if (analysis == null) throw new SpeakingAttemptAnalysisException("AI_OUTPUT_INVALID", false, "analysis is missing");
        Set<String> expected = new LinkedHashSet<>(context.criterionKeys());
        Set<String> received = new LinkedHashSet<>(analysis.criteria().stream().map(value -> value.criterionKey()).toList());
        if (!expected.equals(received)) {
            throw new SpeakingAttemptAnalysisException("AI_OUTPUT_INVALID", false,
                    "analysis criteria do not match the locked lesson criteria");
        }
        int failures = (int) analysis.criteria().stream().filter(value -> !value.satisfied()).count();
        return new ValidatedAttemptAnalysis(analysis, failures);
    }

    public List<String> failedCriterionKeys() {
        return analysis.criteria().stream().filter(value -> !value.satisfied())
                .map(value -> value.criterionKey()).toList();
    }
}
