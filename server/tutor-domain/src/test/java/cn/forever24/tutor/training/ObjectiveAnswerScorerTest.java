package cn.forever24.tutor.training;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectiveAnswerScorerTest {
    private final ObjectiveAnswerScorer scorer = new ObjectiveAnswerScorer();

    @Test
    void acceptsCaseWhitespacePunctuationAndControlledAnswerFrames() {
        assertTrue(scorer.score("  gate 24. ", "Gate 24").correct());
        assertTrue(scorer.score("3:20", "At 3:20").correct());
        assertTrue(scorer.score("It's Gate 24", "Gate 24").correct());
    }

    @Test
    void rejectsSubstringAndExtraContent() {
        assertFalse(scorer.score("Gate 2", "Gate 24").correct());
        assertFalse(scorer.score("Gate 24 please", "Gate 24").correct());
    }
}
