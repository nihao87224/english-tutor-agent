package cn.forever24.tutor.assessment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectiveAssessmentItemTest {

    @Test
    void scoresCorrectOptionAsFullCredit() {
        ObjectiveAssessmentItem item = ObjectiveAssessmentItemBank.requireObjectiveItem("initial-reading-1");

        ObjectiveAnswerScore score = item.score("B");

        assertEquals(AssessmentCorrectness.CORRECT, score.correctness());
        assertEquals("1.0000", score.score().toPlainString());
        assertEquals("1.0000", score.evaluatorConfidence().toPlainString());
    }

    @Test
    void scoresIncorrectOptionAsZeroCredit() {
        ObjectiveAssessmentItem item = ObjectiveAssessmentItemBank.requireObjectiveItem("initial-reading-1");

        ObjectiveAnswerScore score = item.score("C");

        assertEquals(AssessmentCorrectness.INCORRECT, score.correctness());
        assertEquals("0.0000", score.score().toPlainString());
    }

    @Test
    void rejectsUnknownItem() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ObjectiveAssessmentItemBank.requireObjectiveItem("missing-item"));
    }
}
