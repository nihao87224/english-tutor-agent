package cn.forever24.tutor.learner;

import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningEvidenceGeneratorTest {

    @Test
    void createsOneEvidenceDraftPerTargetSkill() {
        List<LearningEvidenceDraft> evidence = LearningEvidenceGenerator.fromTextAttempt(
                task(),
                TaskAttemptSubmission.text(
                        "I think the delay was caused by network instability.",
                        false,
                        0,
                        1200,
                        null,
                        null),
                "I think the delay was caused by network instability.");

        assertEquals(2, evidence.size());
        assertEquals("speaking", evidence.get(0).skillDimension());
        assertEquals(EvidenceType.INDEPENDENT_USE, evidence.get(0).evidenceType());
        assertEquals(EvidenceResult.CORRECT, evidence.get(0).result());
        assertEquals("1.0000", evidence.get(0).independence().toPlainString());
        assertEquals(false, evidence.get(0).metadata().get("rawTextStored"));
    }

    @Test
    void hintLevelReducesIndependenceAndMarksSupportedEvidence() {
        List<LearningEvidenceDraft> evidence = LearningEvidenceGenerator.fromTextAttempt(
                task(),
                TaskAttemptSubmission.text("Short answer.", true, 2, null, null, null),
                "Short answer.");

        assertEquals(EvidenceType.SUPPORTED_CORRECTION, evidence.get(0).evidenceType());
        assertEquals(EvidenceResult.PARTIAL, evidence.get(0).result());
        assertEquals("0.7000", evidence.get(0).independence().toPlainString());
    }

    private static LearningPlanTask task() {
        return new LearningPlanTask(
                "task-1",
                "SPEAKING",
                "Practice a status update",
                10,
                List.of("speaking", "fluency"),
                "A2",
                "Workplace speaking is the weakest skill.");
    }
}
