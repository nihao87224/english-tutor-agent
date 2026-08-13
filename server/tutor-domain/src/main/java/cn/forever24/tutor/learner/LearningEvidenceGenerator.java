package cn.forever24.tutor.learner;

import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.training.TaskAttemptSubmission;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LearningEvidenceGenerator {

    private static final BigDecimal BASE_WEIGHT = new BigDecimal("0.6500");
    private static final BigDecimal BASE_CONFIDENCE = new BigDecimal("0.7000");

    private LearningEvidenceGenerator() {
    }

    public static List<LearningEvidenceDraft> fromTextAttempt(
            LearningPlanTask task,
            TaskAttemptSubmission submission,
            String observedText
    ) {
        if (task == null) {
            throw new IllegalArgumentException("task is required");
        }
        if (submission == null) {
            throw new IllegalArgumentException("submission is required");
        }
        EvidenceType evidenceType = submission.hintLevel() == 0
                ? EvidenceType.INDEPENDENT_USE
                : EvidenceType.SUPPORTED_CORRECTION;
        EvidenceResult result = wordCount(observedText == null ? submission.inputText() : observedText) >= 5
                && submission.hintLevel() <= 1
                ? EvidenceResult.CORRECT
                : EvidenceResult.PARTIAL;
        BigDecimal independence = independence(submission.hintLevel());
        BigDecimal rawScore = result == EvidenceResult.CORRECT
                ? new BigDecimal("0.8200")
                : new BigDecimal("0.6800");
        return task.skillFocus().stream()
                .map(skill -> new LearningEvidenceDraft(
                        skill,
                        knowledgeKey(task, skill),
                        evidenceType,
                        result,
                        rawScore,
                        BASE_WEIGHT,
                        independence,
                        new BigDecimal("0.3000"),
                        0,
                        BASE_CONFIDENCE,
                        Map.of(
                                "taskId", task.taskId(),
                                "taskType", task.type(),
                                "hintLevel", submission.hintLevel(),
                                "rawTextStored", submission.inputText() != null)))
                .toList();
    }

    private static BigDecimal independence(int hintLevel) {
        BigDecimal penalty = new BigDecimal("0.1500").multiply(BigDecimal.valueOf(hintLevel));
        BigDecimal value = BigDecimal.ONE.subtract(penalty);
        if (value.compareTo(new BigDecimal("0.4000")) < 0) {
            return new BigDecimal("0.4000");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.strip().split("\\s+").length;
    }

    private static String knowledgeKey(LearningPlanTask task, String skill) {
        String title = task.title().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String key = skill.toLowerCase(Locale.ROOT) + ":" + task.type().toLowerCase(Locale.ROOT) + ":" + title;
        if (key.length() > 150) {
            return key.substring(0, 150);
        }
        return key;
    }
}
