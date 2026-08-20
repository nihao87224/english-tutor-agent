package cn.forever24.tutor.curriculum;

public record ReviewTemplate(String trigger, TrainingType taskType, String retrievalGoal) {

    public ReviewTemplate {
        if (trigger == null || trigger.isBlank()) {
            throw new IllegalArgumentException("review trigger is required");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("review taskType is required");
        }
        if (retrievalGoal == null || retrievalGoal.isBlank()) {
            throw new IllegalArgumentException("review retrievalGoal is required");
        }
        trigger = trigger.strip();
        retrievalGoal = retrievalGoal.strip();
    }
}
