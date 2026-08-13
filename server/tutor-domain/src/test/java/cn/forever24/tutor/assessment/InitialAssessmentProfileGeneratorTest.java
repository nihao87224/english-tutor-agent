package cn.forever24.tutor.assessment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InitialAssessmentProfileGeneratorTest {

    @Test
    void generatesEightDimensionInitialProfileFromSparseEvidence() {
        AssessmentResult result = InitialAssessmentProfileGenerator.generate(
                "assessment-1",
                List.of(
                        new AssessmentAttemptEvidence(
                                "initial-reading-1",
                                AssessmentCorrectness.CORRECT,
                                BigDecimal.ONE,
                                BigDecimal.ONE),
                        new AssessmentAttemptEvidence(
                                "initial-speaking-open-1",
                                AssessmentCorrectness.PARTIAL,
                                new BigDecimal("0.6500"),
                                new BigDecimal("0.8000"))));

        assertEquals("assessment-1", result.assessmentId());
        assertEquals(8, result.skills().size());
        assertEquals("B1", result.overallLevel());
        assertEquals("100.0000", result.skills().get("reading").score().toPlainString());
        assertEquals("UNDETERMINED", result.skills().get("writing").level());
        assertEquals(3, result.priorities().size());
        assertEquals("MEDIUM", result.recommendedStartingDifficulty());
    }

    @Test
    void rejectsCompletionWithoutAssessmentAttempts() {
        assertThrows(IllegalArgumentException.class,
                () -> InitialAssessmentProfileGenerator.generate("assessment-1", List.of()));
    }
}
