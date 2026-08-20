package cn.forever24.tutor.api.training;

import cn.forever24.tutor.training.LessonSession;

public record LessonSessionResponse(
        String sessionId,
        String prescriptionId,
        int prescriptionVersion,
        String blockId,
        String status,
        ResourceReference resource,
        String skillUnitVariantId,
        String episodeMappingId,
        String inputMode,
        String currentStep,
        StepPayload step,
        Progress progress,
        long version
) {

    static LessonSessionResponse from(LessonSession session) {
        return new LessonSessionResponse(
                session.sessionId(), session.prescriptionId(), session.prescriptionVersion(), session.blockId(),
                session.status().name(),
                new ResourceReference(session.resourceId(), session.resourceVersion()),
                session.skillUnitVariantId(), session.episodeMappingId(), session.inputMode().name(),
                session.currentStep().name(),
                new StepPayload(
                        session.currentStep().name(),
                        session.currentStep().completionMode().name(),
                        session.currentStep().clientCompletable()),
                new Progress(session.completedSteps().size(), session.totalRequiredSteps()),
                session.version());
    }

    public record ResourceReference(String resourceId, String resourceVersion) {
    }

    public record StepPayload(String stepId, String completionMode, boolean clientCompletable) {
    }

    public record Progress(int completedSteps, int totalRequiredSteps) {
    }
}
