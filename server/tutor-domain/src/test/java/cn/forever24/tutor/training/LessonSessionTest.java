package cn.forever24.tutor.training;

import cn.forever24.tutor.curriculum.TrainingType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonSessionTest {

    @Test
    void serverPolicyBuildsRolePlayJourneyAndOnlyAcknowledgesDeterministicCurrentStep() {
        LessonSession session = rolePlaySession();

        LessonSession afterContext = session.completeDeterministicStep(LessonStep.SCENE_CONTEXT);
        LessonSession afterListen = afterContext.completeDeterministicStep(LessonStep.FIRST_LISTEN);

        assertEquals(LessonStep.COMPREHENSION, afterListen.currentStep());
        assertEquals(2, afterListen.completedSteps().size());
        assertTrue(afterListen.requiredSteps().contains(LessonStep.ROLE_PLAY));
        assertThrows(IllegalStateException.class,
                () -> afterListen.completeDeterministicStep(LessonStep.COMPREHENSION));
    }

    @Test
    void rejectsOutOfOrderStepAndResumesAtServerCurrentStep() {
        LessonSession session = rolePlaySession().completeDeterministicStep(LessonStep.SCENE_CONTEXT);

        assertThrows(IllegalStateException.class,
                () -> session.completeDeterministicStep(LessonStep.TRANSCRIPT_EXPRESSIONS));
        LessonSession resumed = session.pause(Instant.parse("2026-08-20T02:01:00Z")).resume();

        assertEquals(LessonSessionStatus.IN_PROGRESS, resumed.status());
        assertEquals(LessonStep.FIRST_LISTEN, resumed.currentStep());
        assertEquals(4, resumed.version());
    }

    private static LessonSession rolePlaySession() {
        return LessonSession.start(
                "lsn_1", "prx_1", 3, "blk_1", "resource-1", "1.0.0",
                "skill.a2", "mapping-1", TrainingType.ROLE_PLAY,
                LessonInputMode.VOICE_OR_TEXT, Instant.parse("2026-08-20T02:00:00Z"));
    }
}
