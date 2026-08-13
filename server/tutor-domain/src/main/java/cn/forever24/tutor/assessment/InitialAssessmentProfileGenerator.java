package cn.forever24.tutor.assessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InitialAssessmentProfileGenerator {

    private static final BigDecimal DEFAULT_SCORE = new BigDecimal("0.5000");
    private static final BigDecimal DEFAULT_CONFIDENCE = new BigDecimal("0.2000");
    private static final BigDecimal MAX_CONFIDENCE = new BigDecimal("0.8200");

    private InitialAssessmentProfileGenerator() {
    }

    public static AssessmentResult generate(String assessmentId, List<AssessmentAttemptEvidence> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            throw new IllegalArgumentException("at least one assessment attempt is required");
        }

        Map<AssessmentSkillDimension, List<AssessmentAttemptEvidence>> evidenceByDimension = new EnumMap<>(
                AssessmentSkillDimension.class);
        for (AssessmentSkillDimension dimension : AssessmentSkillDimension.values()) {
            evidenceByDimension.put(dimension, new ArrayList<>());
        }
        for (AssessmentAttemptEvidence attempt : attempts) {
            for (AssessmentSkillDimension dimension : AssessmentSkillDimension.dimensionsForItem(attempt.itemId())) {
                evidenceByDimension.get(dimension).add(attempt);
            }
        }

        Map<String, AssessmentSkillScore> skills = new LinkedHashMap<>();
        for (AssessmentSkillDimension dimension : AssessmentSkillDimension.values()) {
            skills.put(dimension.contractName(), scoreDimension(dimension, evidenceByDimension.get(dimension)));
        }

        BigDecimal averageScore = averageSkillEstimate(skills);
        BigDecimal overallConfidence = averageConfidence(skills);
        return new AssessmentResult(
                assessmentId,
                levelForEstimate(averageScore),
                skills,
                overallConfidence,
                buildSummary(averageScore, overallConfidence),
                pickStrengths(skills),
                pickPriorities(skills),
                difficultyForEstimate(averageScore));
    }

    private static AssessmentSkillScore scoreDimension(
            AssessmentSkillDimension dimension,
            List<AssessmentAttemptEvidence> attempts
    ) {
        if (attempts.isEmpty()) {
            return new AssessmentSkillScore(
                    toPercent(DEFAULT_SCORE),
                    "UNDETERMINED",
                    DEFAULT_CONFIDENCE,
                    List.of("当前证据不足，后续训练会继续校准。"));
        }

        BigDecimal weightedTotal = BigDecimal.ZERO;
        BigDecimal confidenceTotal = BigDecimal.ZERO;
        List<String> evidence = new ArrayList<>();
        for (AssessmentAttemptEvidence attempt : attempts) {
            BigDecimal weight = attempt.evaluatorConfidence().max(new BigDecimal("0.2500"));
            weightedTotal = weightedTotal.add(attempt.score().multiply(weight));
            confidenceTotal = confidenceTotal.add(weight);
            if (evidence.size() < 5) {
                evidence.add(evidenceText(dimension, attempt));
            }
        }
        BigDecimal estimate = weightedTotal.divide(confidenceTotal, 4, RoundingMode.HALF_UP);
        BigDecimal confidence = DEFAULT_CONFIDENCE
                .add(new BigDecimal("0.1800").multiply(BigDecimal.valueOf(attempts.size())))
                .add(averageAttemptConfidence(attempts).multiply(new BigDecimal("0.2800")))
                .min(MAX_CONFIDENCE)
                .setScale(4, RoundingMode.HALF_UP);
        return new AssessmentSkillScore(toPercent(estimate), levelForEstimate(estimate), confidence, evidence);
    }

    private static BigDecimal averageAttemptConfidence(List<AssessmentAttemptEvidence> attempts) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentAttemptEvidence attempt : attempts) {
            total = total.add(attempt.evaluatorConfidence());
        }
        return total.divide(BigDecimal.valueOf(attempts.size()), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal averageSkillEstimate(Map<String, AssessmentSkillScore> skills) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentSkillScore skill : skills.values()) {
            total = total.add(skill.score().divide(new BigDecimal("100.0000"), 4, RoundingMode.HALF_UP));
        }
        return total.divide(BigDecimal.valueOf(skills.size()), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal averageConfidence(Map<String, AssessmentSkillScore> skills) {
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentSkillScore skill : skills.values()) {
            total = total.add(skill.confidence());
        }
        return total.divide(BigDecimal.valueOf(skills.size()), 4, RoundingMode.HALF_UP);
    }

    private static List<String> pickStrengths(Map<String, AssessmentSkillScore> skills) {
        return skills.entrySet().stream()
                .filter(entry -> entry.getValue().confidence().compareTo(DEFAULT_CONFIDENCE) > 0)
                .sorted(Comparator.comparing(entry -> entry.getValue().score(), Comparator.reverseOrder()))
                .limit(3)
                .map(entry -> displayName(entry.getKey()) + " 是当前相对优势。")
                .toList();
    }

    private static List<String> pickPriorities(Map<String, AssessmentSkillScore> skills) {
        return skills.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().score()))
                .limit(3)
                .map(entry -> displayName(entry.getKey()) + " 建议优先训练。")
                .toList();
    }

    private static String levelForEstimate(BigDecimal estimate) {
        if (estimate.compareTo(new BigDecimal("0.8500")) >= 0) {
            return "C1";
        }
        if (estimate.compareTo(new BigDecimal("0.7000")) >= 0) {
            return "B2";
        }
        if (estimate.compareTo(new BigDecimal("0.5500")) >= 0) {
            return "B1";
        }
        if (estimate.compareTo(new BigDecimal("0.4000")) >= 0) {
            return "A2";
        }
        return "A1";
    }

    private static String difficultyForEstimate(BigDecimal estimate) {
        if (estimate.compareTo(new BigDecimal("0.7000")) >= 0) {
            return "HARD";
        }
        if (estimate.compareTo(new BigDecimal("0.4500")) >= 0) {
            return "MEDIUM";
        }
        return "EASY";
    }

    private static BigDecimal toPercent(BigDecimal estimate) {
        return estimate.multiply(new BigDecimal("100.0000")).setScale(4, RoundingMode.HALF_UP);
    }

    private static String evidenceText(AssessmentSkillDimension dimension, AssessmentAttemptEvidence attempt) {
        return displayName(dimension.contractName()) + " 来源于 " + attempt.itemId()
                + "，结果为 " + attempt.correctness().name() + "。";
    }

    private static String displayName(String dimension) {
        return switch (dimension) {
            case "listening" -> "听力";
            case "speaking" -> "口语";
            case "reading" -> "阅读";
            case "writing" -> "写作";
            case "grammar" -> "语法";
            case "vocabulary" -> "词汇";
            case "fluency" -> "流畅度";
            case "naturalness" -> "自然度";
            default -> dimension;
        };
    }

    private static String buildSummary(BigDecimal averageScore, BigDecimal confidence) {
        return "这是基于初评题目的初始画像，当前等级估计为 "
                + levelForEstimate(averageScore)
                + "，置信度 "
                + confidence.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)
                + "%。后续训练证据会持续更新画像。";
    }
}
