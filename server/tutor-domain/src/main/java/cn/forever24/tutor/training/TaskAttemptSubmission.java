package cn.forever24.tutor.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public record TaskAttemptSubmission(
        TaskAttemptInputType inputType,
        String inputText,
        String textHash,
        int hintLevel,
        Integer clientDurationMs,
        Instant clientStartedAt,
        Instant clientCompletedAt
) {

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final int MAX_HINT_LEVEL = 4;
    private static final int MAX_CLIENT_DURATION_MS = 2 * 60 * 60 * 1000;

    public TaskAttemptSubmission {
        if (inputType == null) {
            throw new IllegalArgumentException("inputType is required");
        }
        if (inputType != TaskAttemptInputType.TEXT) {
            throw new IllegalArgumentException("only TEXT task attempts are supported in M2-T02");
        }
        if (textHash == null || textHash.isBlank()) {
            throw new IllegalArgumentException("textHash is required");
        }
        if (inputText != null && inputText.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("text must be at most 4000 characters");
        }
        if (hintLevel < 0 || hintLevel > MAX_HINT_LEVEL) {
            throw new IllegalArgumentException("hintLevel must be between 0 and 4");
        }
        if (clientDurationMs != null
                && (clientDurationMs < 0 || clientDurationMs > MAX_CLIENT_DURATION_MS)) {
            throw new IllegalArgumentException("clientDurationMs is out of range");
        }
        if (clientStartedAt != null && clientCompletedAt != null
                && clientCompletedAt.isBefore(clientStartedAt)) {
            throw new IllegalArgumentException("clientCompletedAt must not be before clientStartedAt");
        }
    }

    public static TaskAttemptSubmission text(
            String text,
            boolean storeRawText,
            Integer hintLevel,
            Integer clientDurationMs,
            Instant clientStartedAt,
            Instant clientCompletedAt
    ) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("text must be at most 4000 characters");
        }
        return new TaskAttemptSubmission(
                TaskAttemptInputType.TEXT,
                storeRawText ? text : null,
                sha256(text),
                hintLevel == null ? 0 : hintLevel,
                clientDurationMs,
                clientStartedAt,
                clientCompletedAt);
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
