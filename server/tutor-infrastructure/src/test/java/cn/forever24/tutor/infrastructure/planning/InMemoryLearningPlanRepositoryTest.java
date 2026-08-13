package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InMemoryLearningPlanRepositoryTest {

    private final InMemoryLearningPlanRepository repository = new InMemoryLearningPlanRepository();

    @Test
    void trainingCompletionChangesNextPlanOnce() {
        UserKey userKey = new UserKey("user-1");
        LocalDate planDate = LocalDate.parse("2026-08-10");

        LearningPlan first = repository.getOrGenerateTodayPlan(userKey, planDate);
        LearningPlan repeatedBeforeTraining = repository.getOrGenerateTodayPlan(userKey, planDate);

        repository.recordTrainingCompletion(userKey, first.planId(), List.of("speaking"), 1);
        LearningPlan afterTraining = repository.getOrGenerateTodayPlan(userKey, planDate);
        repository.recordTrainingCompletion(userKey, first.planId(), List.of("speaking"), 1);
        LearningPlan repeatedAfterTraining = repository.getOrGenerateTodayPlan(userKey, planDate);

        assertEquals(first.planId(), repeatedBeforeTraining.planId());
        assertNotEquals(first.planId(), afterTraining.planId());
        assertEquals("speaking", first.tasks().get(0).skillFocus().get(0));
        assertEquals("listening", afterTraining.tasks().get(0).skillFocus().get(0));
        assertEquals(afterTraining.planId(), repeatedAfterTraining.planId());
    }
}
