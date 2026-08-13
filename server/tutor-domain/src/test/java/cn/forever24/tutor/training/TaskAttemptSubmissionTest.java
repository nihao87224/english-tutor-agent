package cn.forever24.tutor.training;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskAttemptSubmissionTest {

    @Test
    void textSubmissionStoresRawTextOnlyWhenAllowed() {
        TaskAttemptSubmission stored = TaskAttemptSubmission.text(
                "I think the delay was caused by network instability.",
                true,
                1,
                1200,
                Instant.parse("2026-08-10T08:00:00Z"),
                Instant.parse("2026-08-10T08:00:02Z"));
        TaskAttemptSubmission privateSubmission = TaskAttemptSubmission.text(
                "I think the delay was caused by network instability.",
                false,
                1,
                1200,
                null,
                null);

        assertEquals("I think the delay was caused by network instability.", stored.inputText());
        assertNull(privateSubmission.inputText());
        assertEquals(stored.textHash(), privateSubmission.textHash());
    }

    @Test
    void rejectsInvalidTextAttemptInput() {
        assertThrows(IllegalArgumentException.class,
                () -> TaskAttemptSubmission.text(" ", true, 0, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> TaskAttemptSubmission.text("hello", true, 5, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> TaskAttemptSubmission.text(
                        "hello",
                        true,
                        0,
                        null,
                        Instant.parse("2026-08-10T08:00:02Z"),
                        Instant.parse("2026-08-10T08:00:01Z")));
    }
}
