package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.planning.LearnerSkillState;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanContext;
import cn.forever24.tutor.planning.RuleBasedTodayPlanGenerator;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryLearningPlanRepository implements LearningPlanRepository {

    private final Map<String, LearningPlan> plans = new ConcurrentHashMap<>();
    private final Map<String, String> planOwners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<LearnerSkillState>> skillStatesByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> planningVersionByUser = new ConcurrentHashMap<>();
    private final Set<String> recordedCompletions = ConcurrentHashMap.newKeySet();

    @Override
    public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
        long planningVersion = planningVersionByUser
                .computeIfAbsent(userKey.value(), ignored -> new AtomicLong(1))
                .get();
        String key = userKey.value() + ":" + planDate + ":" + planningVersion + ":0";
        LearningPlan plan = plans.computeIfAbsent(key, ignored -> RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                "plan-" + Math.abs(key.hashCode()),
                planDate,
                PrimaryGoal.GENERAL,
                20,
                planningVersion,
                skillStatesFor(userKey.value()))));
        planOwners.putIfAbsent(plan.planId(), userKey.value());
        return plan;
    }

    @Override
    public LearningPlan getPlan(UserKey userKey, String planId) {
        for (LearningPlan plan : plans.values()) {
            if (plan.planId().equals(planId) && userKey.value().equals(planOwners.get(planId))) {
                return plan;
            }
        }
        throw new IllegalArgumentException("learning plan was not found");
    }

    @Override
    public void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId is required");
        }
        if (!userKey.value().equals(planOwners.get(planId))) {
            throw new IllegalArgumentException("learning plan was not found");
        }
        if (practicedSkills == null || practicedSkills.isEmpty() || evidenceCount <= 0) {
            throw new IllegalArgumentException("accepted learning evidence is required before planning can change");
        }
        String completionKey = userKey.value() + ":" + planId;
        if (!recordedCompletions.add(completionKey)) {
            return;
        }

        List<String> practiced = practicedSkills.stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .map(skill -> skill.strip().toLowerCase())
                .distinct()
                .toList();
        skillStatesByUser.compute(userKey.value(), (ignored, existing) -> updateSkillStates(existing, practiced, evidenceCount));
        planningVersionByUser
                .computeIfAbsent(userKey.value(), ignored -> new AtomicLong(1))
                .incrementAndGet();
    }

    private List<LearnerSkillState> skillStatesFor(String userKey) {
        return skillStatesByUser.computeIfAbsent(userKey, ignored -> defaultSkillStates());
    }

    private static List<LearnerSkillState> updateSkillStates(
            List<LearnerSkillState> existing,
            List<String> practicedSkills,
            int evidenceCount
    ) {
        List<LearnerSkillState> source = existing == null ? defaultSkillStates() : existing;
        List<LearnerSkillState> updated = new ArrayList<>();
        for (LearnerSkillState state : source) {
            if (practicedSkills.contains(state.dimension())) {
                BigDecimal updatedEstimate = state.estimate().add(new BigDecimal("0.1200")).min(BigDecimal.ONE);
                updated.add(new LearnerSkillState(
                        state.dimension(),
                        updatedEstimate,
                        state.confidence(),
                        levelFor(updatedEstimate),
                        state.evidenceCount() + evidenceCount));
            } else {
                updated.add(state);
            }
        }
        return List.copyOf(updated);
    }

    private static List<LearnerSkillState> defaultSkillStates() {
        return List.of(
                new LearnerSkillState("speaking", new BigDecimal("0.4200"), new BigDecimal("0.6500"), "A2", 1),
                new LearnerSkillState("listening", new BigDecimal("0.5000"), new BigDecimal("0.5000"), "A2", 1),
                new LearnerSkillState("grammar", new BigDecimal("0.5600"), new BigDecimal("0.5000"), "B1", 1));
    }

    private static String levelFor(BigDecimal estimate) {
        if (estimate.compareTo(new BigDecimal("0.7000")) >= 0) {
            return "B2";
        }
        if (estimate.compareTo(new BigDecimal("0.4500")) >= 0) {
            return "B1";
        }
        return "A2";
    }
}
