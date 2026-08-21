package cn.forever24.tutor.training;

import java.text.Normalizer;
import java.util.Locale;

public final class ObjectiveAnswerScorer {

    public LessonObjectiveResult score(String answer, String expectedAnswer) {
        String actual = normalize(answer);
        String expected = normalize(expectedAnswer);
        boolean correct = actual.equals(expected) || stripOptionalFrame(actual).equals(stripOptionalFrame(expected));
        return new LessonObjectiveResult(
                correct,
                expectedAnswer.strip(),
                correct ? "Answer confirmed." : "Review the scene detail and compare it with the expected answer.");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("answer is required");
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("[.!?。！？]+$", "")
                .strip();
    }

    private static String stripOptionalFrame(String value) {
        return value.replaceFirst("^(it(?:'s| is)|the answer is|at)\\s+", "");
    }
}
