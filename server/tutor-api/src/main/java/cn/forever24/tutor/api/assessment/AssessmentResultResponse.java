package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.assessment.AssessmentSkillScore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AssessmentResultResponse(
        String assessmentId,
        String overallLevel,
        Map<String, SkillScoreResponse> skills,
        BigDecimal confidence,
        String summaryZh,
        List<String> strengths,
        List<String> priorities,
        String recommendedStartingDifficulty
) {

    public static AssessmentResultResponse from(AssessmentResult result) {
        return new AssessmentResultResponse(
                result.assessmentId(),
                result.overallLevel(),
                result.skills().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> SkillScoreResponse.from(entry.getValue()),
                                (left, right) -> left,
                                java.util.LinkedHashMap::new)),
                result.confidence(),
                result.summaryZh(),
                result.strengths(),
                result.priorities(),
                result.recommendedStartingDifficulty());
    }

    public record SkillScoreResponse(
            BigDecimal score,
            String level,
            BigDecimal confidence,
            List<String> evidence
    ) {

        private static SkillScoreResponse from(AssessmentSkillScore score) {
            return new SkillScoreResponse(score.score(), score.level(), score.confidence(), score.evidence());
        }
    }
}
