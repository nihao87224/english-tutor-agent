package cn.forever24.tutor.planning;

import java.util.Comparator;
import java.util.List;

public record LearnerInputSnapshot(
        long profileVersion,
        int availableMinutes,
        String primaryGoal,
        String temporaryGoal,
        List<PrescriptionSkillState> skillStates
) {

    public LearnerInputSnapshot {
        if (profileVersion < 0 || availableMinutes < 1 || availableMinutes > 480) {
            throw new IllegalArgumentException("valid profileVersion and availableMinutes are required");
        }
        if (primaryGoal == null || primaryGoal.isBlank()) {
            throw new IllegalArgumentException("primaryGoal is required");
        }
        primaryGoal = primaryGoal.strip();
        if (temporaryGoal != null) {
            if (temporaryGoal.isBlank() || temporaryGoal.strip().length() > 500) {
                throw new IllegalArgumentException("temporaryGoal is invalid");
            }
            temporaryGoal = temporaryGoal.strip();
        }
        if (skillStates == null || skillStates.isEmpty() || skillStates.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("skillStates must not be empty");
        }
        if (skillStates.stream().map(PrescriptionSkillState::skillKey).distinct().count() != skillStates.size()) {
            throw new IllegalArgumentException("skillState keys must be unique");
        }
        skillStates = skillStates.stream()
                .sorted(Comparator.comparing(PrescriptionSkillState::skillKey))
                .toList();
    }
}
