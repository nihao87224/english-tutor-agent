package cn.forever24.tutor.assessment;

import java.util.List;

public enum AssessmentSkillDimension {
    LISTENING("listening"),
    SPEAKING("speaking"),
    READING("reading"),
    WRITING("writing"),
    GRAMMAR("grammar"),
    VOCABULARY("vocabulary"),
    FLUENCY("fluency"),
    NATURALNESS("naturalness");

    private final String contractName;

    AssessmentSkillDimension(String contractName) {
        this.contractName = contractName;
    }

    public String contractName() {
        return contractName;
    }

    public static List<AssessmentSkillDimension> dimensionsForItem(String itemId) {
        return switch (itemId) {
            case "initial-listening-1" -> List.of(LISTENING);
            case "initial-reading-1" -> List.of(READING);
            case "initial-grammar-1" -> List.of(GRAMMAR, VOCABULARY);
            case "initial-speaking-open-1" -> List.of(SPEAKING, FLUENCY, NATURALNESS);
            case "initial-writing-open-1" -> List.of(WRITING, GRAMMAR, VOCABULARY);
            default -> List.of();
        };
    }
}
