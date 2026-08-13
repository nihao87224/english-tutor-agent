package cn.forever24.tutor.assessment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentSessionTest {

    @Test
    void resolvesMissingTargetToDefaultMinutes() {
        assertEquals(9, AssessmentSession.resolveTargetMinutes(null));
    }

    @Test
    void acceptsTargetBounds() {
        assertEquals(5, AssessmentSession.resolveTargetMinutes(5));
        assertEquals(15, AssessmentSession.resolveTargetMinutes(15));
    }

    @Test
    void rejectsTargetOutsideBounds() {
        assertThrows(IllegalArgumentException.class, () -> AssessmentSession.resolveTargetMinutes(4));
        assertThrows(IllegalArgumentException.class, () -> AssessmentSession.resolveTargetMinutes(16));
    }

    @Test
    void rejectsNegativeRemainingMinutes() {
        assertThrows(IllegalArgumentException.class, () -> new AssessmentSession(
                "assessment-1",
                AssessmentSessionStatus.IN_PROGRESS,
                9,
                -1));
    }
}
