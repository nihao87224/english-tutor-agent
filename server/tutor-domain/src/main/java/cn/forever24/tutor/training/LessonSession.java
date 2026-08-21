package cn.forever24.tutor.training;

import cn.forever24.tutor.curriculum.TrainingType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record LessonSession(
        String sessionId,
        String prescriptionId,
        int prescriptionVersion,
        String blockId,
        String resourceId,
        String resourceVersion,
        String skillUnitVariantId,
        String episodeMappingId,
        LessonInputMode inputMode,
        LessonSessionStatus status,
        LessonStep currentStep,
        List<LessonStep> requiredSteps,
        List<LessonStep> completedSteps,
        Instant startedAt,
        Instant pausedAt,
        Instant completedAt,
        long version
) {

    public LessonSession {
        requireText(sessionId, "sessionId");
        requireText(prescriptionId, "prescriptionId");
        if (prescriptionVersion < 1) {
            throw new IllegalArgumentException("prescriptionVersion must be positive");
        }
        requireText(blockId, "blockId");
        requireText(resourceId, "resourceId");
        requireText(resourceVersion, "resourceVersion");
        requireText(skillUnitVariantId, "skillUnitVariantId");
        requireText(episodeMappingId, "episodeMappingId");
        Objects.requireNonNull(inputMode, "inputMode is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(currentStep, "currentStep is required");
        requiredSteps = List.copyOf(requiredSteps);
        completedSteps = List.copyOf(completedSteps);
        if (requiredSteps.isEmpty() || requiredSteps.getFirst() != LessonStep.SCENE_CONTEXT
                || requiredSteps.getLast() != LessonStep.COMPLETE) {
            throw new IllegalArgumentException("requiredSteps must span SCENE_CONTEXT to COMPLETE");
        }
        if (!requiredSteps.contains(currentStep) || !requiredSteps.containsAll(completedSteps)) {
            throw new IllegalArgumentException("session steps must belong to requiredSteps");
        }
        Objects.requireNonNull(startedAt, "startedAt is required");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }

    public static LessonSession start(
            String sessionId,
            String prescriptionId,
            int prescriptionVersion,
            String blockId,
            String resourceId,
            String resourceVersion,
            String skillUnitVariantId,
            String episodeMappingId,
            TrainingType trainingType,
            LessonInputMode inputMode,
            Instant now
    ) {
        List<LessonStep> steps = stepsFor(trainingType);
        return new LessonSession(
                sessionId, prescriptionId, prescriptionVersion, blockId,
                resourceId, resourceVersion, skillUnitVariantId, episodeMappingId,
                inputMode == null ? LessonInputMode.VOICE_OR_TEXT : inputMode,
                LessonSessionStatus.IN_PROGRESS, steps.getFirst(), steps, List.of(),
                now, null, null, 1);
    }

    public LessonSession pause(Instant now) {
        if (status == LessonSessionStatus.PAUSED) {
            return this;
        }
        requireStatus(LessonSessionStatus.IN_PROGRESS, "pause");
        return copy(LessonSessionStatus.PAUSED, currentStep, completedSteps, now, completedAt);
    }

    public LessonSession resume() {
        if (status == LessonSessionStatus.IN_PROGRESS) {
            return this;
        }
        requireStatus(LessonSessionStatus.PAUSED, "resume");
        return copy(LessonSessionStatus.IN_PROGRESS, currentStep, completedSteps, null, completedAt);
    }

    public LessonSession completeDeterministicStep(LessonStep step) {
        requireStatus(LessonSessionStatus.IN_PROGRESS, "complete a step");
        if (step != currentStep) {
            throw new IllegalStateException("only the current step can be completed");
        }
        if (!step.clientCompletable()) {
            throw new IllegalStateException("step completion requires server evidence or an attempt");
        }
        int index = requiredSteps.indexOf(step);
        if (index < 0 || index + 1 >= requiredSteps.size()) {
            throw new IllegalStateException("lesson cannot be completed by a client step acknowledgement");
        }
        List<LessonStep> completed = new ArrayList<>(completedSteps);
        if (!completed.contains(step)) {
            completed.add(step);
        }
        return copy(status, requiredSteps.get(index + 1), completed, pausedAt, completedAt);
    }

    public LessonSession completeAttemptStep(LessonStep step) {
        requireStatus(LessonSessionStatus.IN_PROGRESS, "complete an attempt step");
        if (step != currentStep || step.completionMode() != LessonStep.CompletionMode.ATTEMPT_REQUIRED) {
            throw new IllegalStateException("only the current attempt-required step can be completed");
        }
        int index = requiredSteps.indexOf(step);
        if (index < 0 || index + 1 >= requiredSteps.size()) {
            throw new IllegalStateException("attempt step cannot advance this lesson");
        }
        List<LessonStep> completed = new ArrayList<>(completedSteps);
        if (!completed.contains(step)) {
            completed.add(step);
        }
        return copy(status, requiredSteps.get(index + 1), completed, pausedAt, completedAt);
    }

    public int totalRequiredSteps() {
        return requiredSteps.size() - 1;
    }

    private LessonSession copy(
            LessonSessionStatus nextStatus,
            LessonStep nextStep,
            List<LessonStep> nextCompleted,
            Instant nextPausedAt,
            Instant nextCompletedAt
    ) {
        return new LessonSession(
                sessionId, prescriptionId, prescriptionVersion, blockId,
                resourceId, resourceVersion, skillUnitVariantId, episodeMappingId,
                inputMode, nextStatus, nextStep, requiredSteps, nextCompleted,
                startedAt, nextPausedAt, nextCompletedAt, version + 1);
    }

    private void requireStatus(LessonSessionStatus expected, String operation) {
        if (status != expected) {
            throw new IllegalStateException("lesson session must be " + expected + " to " + operation);
        }
    }

    private static List<LessonStep> stepsFor(TrainingType trainingType) {
        Objects.requireNonNull(trainingType, "trainingType is required");
        return switch (trainingType) {
            case COMPREHENSION -> List.of(
                    LessonStep.SCENE_CONTEXT, LessonStep.FIRST_LISTEN, LessonStep.COMPREHENSION,
                    LessonStep.TRANSCRIPT_EXPRESSIONS, LessonStep.EVIDENCE, LessonStep.COMPLETE);
            case GUIDED_SPEAKING, REVIEW -> List.of(
                    LessonStep.SCENE_CONTEXT, LessonStep.FIRST_LISTEN, LessonStep.COMPREHENSION,
                    LessonStep.TRANSCRIPT_EXPRESSIONS, LessonStep.GUIDED_SPEAKING,
                    LessonStep.FEEDBACK, LessonStep.EVIDENCE, LessonStep.COMPLETE);
            case ROLE_PLAY, TRANSFER -> List.of(
                    LessonStep.SCENE_CONTEXT, LessonStep.FIRST_LISTEN, LessonStep.COMPREHENSION,
                    LessonStep.TRANSCRIPT_EXPRESSIONS, LessonStep.GUIDED_SPEAKING,
                    LessonStep.ROLE_PLAY, LessonStep.FEEDBACK, LessonStep.EVIDENCE, LessonStep.COMPLETE);
        };
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
