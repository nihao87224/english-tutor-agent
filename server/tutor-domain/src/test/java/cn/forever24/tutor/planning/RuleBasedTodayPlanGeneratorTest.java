package cn.forever24.tutor.planning;

import cn.forever24.tutor.profile.PrimaryGoal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleBasedTodayPlanGeneratorTest {

    @Test
    void createsWorkplacePlanFromWeakestSkillStates() {
        LearningPlan plan = RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                "plan-1",
                LocalDate.parse("2026-08-06"),
                PrimaryGoal.WORKPLACE,
                20,
                3,
                List.of(
                        new LearnerSkillState("speaking", new BigDecimal("0.3600"), new BigDecimal("0.7000"), "A1", 1),
                        new LearnerSkillState("reading", new BigDecimal("0.7600"), new BigDecimal("0.7000"), "B2", 1),
                        new LearnerSkillState("listening", new BigDecimal("0.4500"), new BigDecimal("0.6000"), "A2", 1),
                        new LearnerSkillState("grammar", new BigDecimal("0.5200"), new BigDecimal("0.5000"), "A2", 1))));

        assertEquals("plan-1", plan.planId());
        assertEquals(20, plan.totalMinutes());
        assertEquals(3, plan.tasks().size());
        assertEquals("CONVERSATION", plan.tasks().get(0).type());
        assertEquals(List.of("speaking"), plan.tasks().get(0).skillFocus());
        assertEquals("EASY", plan.tasks().get(0).difficulty());
        assertEquals(false, plan.temporaryAdjustment());
    }

    @Test
    void keepsFiveMinutePlanToOneHighValueTask() {
        LearningPlan plan = RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                "plan-quick",
                LocalDate.parse("2026-08-06"),
                PrimaryGoal.GENERAL,
                5,
                1,
                List.of(
                        new LearnerSkillState("writing", new BigDecimal("0.4100"), new BigDecimal("0.6000"), "A2", 1),
                        new LearnerSkillState("listening", new BigDecimal("0.5000"), new BigDecimal("0.5000"), "A2", 1))));

        assertEquals(1, plan.tasks().size());
        assertEquals(5, plan.totalMinutes());
        assertEquals("WRITING", plan.tasks().get(0).type());
    }

    @Test
    void deprioritizesRecentlyPracticedEvidenceBackedSkill() {
        LearningPlan plan = RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                "plan-after-training",
                LocalDate.parse("2026-08-07"),
                PrimaryGoal.GENERAL,
                5,
                2,
                List.of(
                        new LearnerSkillState("speaking", new BigDecimal("0.4200"), new BigDecimal("0.6500"), "A2", 2),
                        new LearnerSkillState("listening", new BigDecimal("0.5000"), new BigDecimal("0.5000"), "A2", 1),
                        new LearnerSkillState("grammar", new BigDecimal("0.5600"), new BigDecimal("0.5000"), "B1", 1))));

        assertEquals("LISTENING", plan.tasks().get(0).type());
        assertEquals(List.of("listening"), plan.tasks().get(0).skillFocus());
    }

    @Test
    void rejectsMissingSkillStates() {
        assertThrows(IllegalArgumentException.class, () -> new LearningPlanContext(
                "plan-1",
                LocalDate.parse("2026-08-06"),
                PrimaryGoal.GENERAL,
                20,
                1,
                List.of()));
    }
}
