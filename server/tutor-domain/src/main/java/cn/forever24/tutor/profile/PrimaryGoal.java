package cn.forever24.tutor.profile;

import java.util.Arrays;

public enum PrimaryGoal {
    WORKPLACE,
    GENERAL,
    IELTS;

    public static PrimaryGoal fromContractValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("primary goal is required");
        }
        return Arrays.stream(values())
                .filter(goal -> goal.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported primary goal: " + value));
    }
}
