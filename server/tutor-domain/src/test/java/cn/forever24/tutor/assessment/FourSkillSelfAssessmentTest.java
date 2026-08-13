package cn.forever24.tutor.assessment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FourSkillSelfAssessmentTest {

    @Test
    void acceptsContractRatingValuesAndEstimatesAverageBand() {
        FourSkillSelfAssessment assessment = FourSkillSelfAssessment.fromContractValues(
                "INTERMEDIATE",
                "UPPER_INTERMEDIATE",
                "INTERMEDIATE",
                "BASIC");

        assertEquals(SelfRating.INTERMEDIATE, assessment.estimatedBand());
    }

    @Test
    void rejectsMissingAndUnsupportedRatings() {
        assertThrows(IllegalArgumentException.class,
                () -> FourSkillSelfAssessment.fromContractValues(null, "BASIC", "BASIC", "BASIC"));
        assertThrows(IllegalArgumentException.class,
                () -> FourSkillSelfAssessment.fromContractValues("EXPERT", "BASIC", "BASIC", "BASIC"));
    }
}
