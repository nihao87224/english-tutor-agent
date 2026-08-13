package cn.forever24.tutor.planning;

import cn.forever24.tutor.profile.PrimaryGoal;

import java.time.LocalDate;
import java.util.List;

public record LearningPlanContext(
        String planId,
        LocalDate date,
        PrimaryGoal primaryGoal,
        int dailyMinutes,
        long profileVersion,
        List<LearnerSkillState> skillStates
) {

    public LearningPlanContext {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        if (primaryGoal == null) {
            throw new IllegalArgumentException("primaryGoal is required");
        }
        if (!List.of(5, 10, 20, 30, 45).contains(dailyMinutes)) {
            throw new IllegalArgumentException("unsupported daily minutes: " + dailyMinutes);
        }
        if (profileVersion <= 0) {
            throw new IllegalArgumentException("profileVersion must be positive");
        }
        skillStates = List.copyOf(skillStates == null ? List.of() : skillStates);
        if (skillStates.isEmpty()) {
            throw new IllegalArgumentException("skill states are required");
        }
    }
}
