package cn.forever24.tutor.reporting;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class DailyTrainingSummaryGenerator {

    private DailyTrainingSummaryGenerator() {
    }

    public static DailyTrainingSummary generate(
            String sessionId,
            int completedTaskCount,
            List<DailySummaryEvidence> evidence,
            Instant generatedAt
    ) {
        List<DailySummaryEvidence> safeEvidence = List.copyOf(evidence == null ? List.of() : evidence);
        if (completedTaskCount <= 0 || safeEvidence.isEmpty()) {
            throw new IllegalArgumentException("accepted attempts and learning evidence are required to summarize");
        }
        List<String> skills = safeEvidence.stream()
                .map(DailySummaryEvidence::skillDimension)
                .distinct()
                .sorted()
                .toList();
        List<String> highlights = skills.stream()
                .map(skill -> "Completed focused " + skill + " practice with traceable evidence.")
                .limit(3)
                .toList();
        List<String> memorableItems = safeEvidence.stream()
                .sorted(Comparator.comparing(DailySummaryEvidence::skillDimension))
                .map(item -> item.knowledgeKey() + " (" + item.result() + ")")
                .distinct()
                .limit(3)
                .toList();
        List<String> nextFocus = skills.stream()
                .map(skill -> "Review one new " + skill + " scenario and try it with fewer hints.")
                .limit(3)
                .toList();
        return new DailyTrainingSummary(
                sessionId,
                completedTaskCount,
                safeEvidence.size(),
                skills,
                highlights,
                memorableItems,
                nextFocus,
                generatedAt);
    }
}
