package cn.forever24.tutor.assessment;

import java.util.List;
import java.util.Set;

/** Fixed, versioned initial-assessment sequence. */
public final class InitialAssessmentItemBank {

    private static final List<AssessmentItem> ITEMS = List.of(
            new AssessmentItem("initial-reading-1", "READING", "MULTIPLE_CHOICE",
                    "Choose the best completion: ‘She ___ to the office every day.’",
                    List.of("A. go", "B. goes", "C. going", "D. gone"), 45),
            new AssessmentItem("initial-listening-1", "LISTENING", "MULTIPLE_CHOICE",
                    "Choose the response that best fits: ‘Could you send me the report by Friday?’",
                    List.of("A. I sent it yesterday.", "B. Friday is a report.", "C. Sure, I will send it before then.", "D. The office is closed."), 45),
            new AssessmentItem("initial-grammar-1", "READING", "MULTIPLE_CHOICE",
                    "Choose the correct sentence.",
                    List.of("A. I have lived here for three years.", "B. I has lived here for three years.", "C. I living here for three years.", "D. I have live here for three years."), 45),
            new AssessmentItem("initial-speaking-open-1", "SPEAKING", "SHORT_TEXT",
                    "In 2–4 sentences, describe a recent challenge at work or in study and how you handled it.",
                    List.of(), 180),
            new AssessmentItem("initial-writing-open-1", "WRITING", "SHORT_TEXT",
                    "Write a short message (3–5 sentences) asking a colleague to reschedule a meeting politely.",
                    List.of(), 240));

    private InitialAssessmentItemBank() {
    }

    public static AssessmentItem nextUnanswered(Set<String> answeredItemIds) {
        return ITEMS.stream().filter(item -> !answeredItemIds.contains(item.itemId())).findFirst().orElse(null);
    }

    public static boolean allAnswered(Set<String> answeredItemIds) {
        return ITEMS.stream().allMatch(item -> answeredItemIds.contains(item.itemId()));
    }
}
