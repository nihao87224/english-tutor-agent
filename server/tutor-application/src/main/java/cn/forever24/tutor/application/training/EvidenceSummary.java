package cn.forever24.tutor.application.training;

import java.util.List;

/** A factual receipt, intentionally distinct from a mastery claim. */
public record EvidenceSummary(String attemptId, int evidenceCount, List<String> affectedSkills, String nextFocus) {
    public EvidenceSummary {
        if (attemptId == null || attemptId.isBlank() || evidenceCount < 1) {
            throw new IllegalArgumentException("evidence summary requires an attempt and evidence");
        }
        affectedSkills = List.copyOf(affectedSkills == null ? List.of() : affectedSkills);
        if (affectedSkills.isEmpty() || nextFocus == null || nextFocus.isBlank()) {
            throw new IllegalArgumentException("evidence summary requires affected skills and next focus");
        }
        nextFocus = nextFocus.strip();
    }
}
