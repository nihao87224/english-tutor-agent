package cn.forever24.tutor.application.planning;

import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.profile.UserKey;

import java.time.LocalDate;
import java.util.List;

public interface LearningPlanRepository {

    LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate);

    LearningPlan getPlan(UserKey userKey, String planId);

    void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount);
}
