package cn.forever24.tutor.assessment;

import java.util.Map;

public final class OpenAssessmentItemBank {

    public static final String CONTENT_VERSION = "assessment-content-v1";
    public static final int MAX_TEXT_LENGTH = 800;

    private static final Map<String, OpenAssessmentItem> ITEMS = Map.of(
            "initial-speaking-open-1", new OpenAssessmentItem("initial-speaking-open-1", "SHORT_TEXT"),
            "initial-writing-open-1", new OpenAssessmentItem("initial-writing-open-1", "SHORT_TEXT")
    );

    private OpenAssessmentItemBank() {
    }

    public static OpenAssessmentItem requireOpenTextItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        OpenAssessmentItem item = ITEMS.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("unknown open text item: " + itemId);
        }
        return item;
    }

    public static String requireAnswerText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text is required for open answers");
        }
        String normalized = text.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("text must be 800 characters or fewer");
        }
        return normalized;
    }
}
