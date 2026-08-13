package cn.forever24.tutor.assessment;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AssessmentResult(
        String assessmentId,
        String overallLevel,
        Map<String, AssessmentSkillScore> skills,
        BigDecimal confidence,
        String summaryZh,
        List<String> strengths,
        List<String> priorities,
        String recommendedStartingDifficulty
) {

    public AssessmentResult {
        if (assessmentId == null || assessmentId.isBlank()) {
            throw new IllegalArgumentException("assessmentId is required");
        }
        if (overallLevel == null || overallLevel.isBlank()) {
            throw new IllegalArgumentException("overallLevel is required");
        }
        if (skills == null || skills.isEmpty()) {
            throw new IllegalArgumentException("skills are required");
        }
        skills = Collections.unmodifiableMap(new LinkedHashMap<>(skills));
        if (confidence == null
                || confidence.compareTo(ObjectiveAnswerScore.ZERO_SCORE) < 0
                || confidence.compareTo(ObjectiveAnswerScore.FULL_SCORE) > 0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        if (summaryZh == null || summaryZh.isBlank() || summaryZh.length() > 800) {
            throw new IllegalArgumentException("summaryZh must be 1-800 characters");
        }
        strengths = List.copyOf(strengths == null ? List.of() : strengths);
        priorities = List.copyOf(priorities == null ? List.of() : priorities);
        if (strengths.size() > 5 || priorities.isEmpty() || priorities.size() > 5) {
            throw new IllegalArgumentException("strengths and priorities must fit contract bounds");
        }
        if (recommendedStartingDifficulty == null || recommendedStartingDifficulty.isBlank()) {
            throw new IllegalArgumentException("recommendedStartingDifficulty is required");
        }
    }
}
