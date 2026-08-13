package cn.forever24.tutor.reporting;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyTrainingSummaryGeneratorTest {

    @Test
    void generatesDeterministicSummaryFromEvidence() {
        DailyTrainingSummary summary = DailyTrainingSummaryGenerator.generate(
                "training-1",
                1,
                List.of(new DailySummaryEvidence(
                        "speaking",
                        "SUPPORTED_CORRECTION",
                        "CORRECT",
                        "speaking:speaking:practice_status_update")),
                Instant.parse("2026-08-10T08:30:00Z"));

        assertEquals("training-1", summary.sessionId());
        assertEquals(1, summary.completedTaskCount());
        assertEquals(1, summary.evidenceCount());
        assertEquals(List.of("speaking"), summary.practicedSkills());
        assertEquals(true, summary.memorableItems().get(0).contains("CORRECT"));
    }

    @Test
    void rejectsSummaryWithoutAttemptsOrEvidence() {
        assertThrows(IllegalArgumentException.class, () -> DailyTrainingSummaryGenerator.generate(
                "training-1",
                0,
                List.of(),
                Instant.parse("2026-08-10T08:30:00Z")));
    }
}
