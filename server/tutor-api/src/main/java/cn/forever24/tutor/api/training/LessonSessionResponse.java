package cn.forever24.tutor.api.training;

import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.application.training.LessonAttemptProgress;

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
        AttemptProgress attemptProgress,
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
                new Progress(session.completedSteps().size(), session.totalRequiredSteps()), null,
                session.version());
    }

    static LessonSessionResponse from(LessonSession session, LessonAttemptProgress attemptProgress) {
        LessonSessionResponse base = from(session);
        return new LessonSessionResponse(
                base.sessionId(), base.prescriptionId(), base.prescriptionVersion(), base.blockId(), base.status(),
                base.resource(), base.skillUnitVariantId(), base.episodeMappingId(), base.inputMode(),
                base.currentStep(), base.step(), base.progress(), AttemptProgress.from(attemptProgress), base.version());
    }

    public record ResourceReference(String resourceId, String resourceVersion) {
    }

    public record StepPayload(String stepId, String completionMode, boolean clientCompletable) {
    }

    public record Progress(int completedSteps, int totalRequiredSteps) {
    }

    public record AttemptProgress(
            String stepId,
            java.util.List<String> completedTaskIds,
            java.util.List<String> remainingTaskIds,
            boolean nextStepEligible,
            String pendingAttemptId
    ) {
        static AttemptProgress from(LessonAttemptProgress progress) {
            return new AttemptProgress(
                    progress.step().name(), progress.completedTaskIds(), progress.remainingTaskIds(),
                    progress.nextStepEligible(), progress.pendingAttemptId());
        }
    }
}
