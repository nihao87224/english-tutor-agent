package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;

import java.time.ZoneId;
import java.util.List;

public record LearnerPlanningSnapshot(
        UserKey userKey,
        PrimaryGoal primaryGoal,
        ZoneId timezone,
        int dailyMinutes,
        long profileVersion,
        CefrLevel currentLevel,
        List<PrescriptionSkillState> skillStates
) {

    public LearnerPlanningSnapshot {
        if (userKey == null || primaryGoal == null || timezone == null || currentLevel == null
                || profileVersion < 0 || dailyMinutes < 1 || dailyMinutes > 480) {
            throw new IllegalArgumentException("valid learner planning snapshot metadata is required");
        }
        if (skillStates == null || skillStates.isEmpty()) {
            throw new IllegalArgumentException("learner skill states are required");
        }
        skillStates = List.copyOf(skillStates);
    }
}
