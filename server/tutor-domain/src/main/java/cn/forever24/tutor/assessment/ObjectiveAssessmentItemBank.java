package cn.forever24.tutor.assessment;

import java.util.Map;

public final class ObjectiveAssessmentItemBank {

    public static final String CONTENT_VERSION = "assessment-content-v1";

    private static final Map<String, ObjectiveAssessmentItem> ITEMS = Map.of(
            "initial-reading-1", new ObjectiveAssessmentItem("initial-reading-1", "MULTIPLE_CHOICE", "B"),
            "initial-listening-1", new ObjectiveAssessmentItem("initial-listening-1", "MULTIPLE_CHOICE", "C"),
            "initial-grammar-1", new ObjectiveAssessmentItem("initial-grammar-1", "MULTIPLE_CHOICE", "A")
    );

    private ObjectiveAssessmentItemBank() {
    }

    public static ObjectiveAssessmentItem requireObjectiveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        ObjectiveAssessmentItem item = ITEMS.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("unknown objective item: " + itemId);
        }
        return item;
    }
}
