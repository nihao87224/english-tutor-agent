package cn.forever24.tutor.reporting;

public record DailySummaryEvidence(
        String skillDimension,
        String evidenceType,
        String result,
        String knowledgeKey
) {

    public DailySummaryEvidence {
        skillDimension = requireNonBlank(skillDimension, "skill dimension").toLowerCase();
        evidenceType = requireNonBlank(evidenceType, "evidence type");
        result = requireNonBlank(result, "result");
        knowledgeKey = requireNonBlank(knowledgeKey, "knowledge key");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
