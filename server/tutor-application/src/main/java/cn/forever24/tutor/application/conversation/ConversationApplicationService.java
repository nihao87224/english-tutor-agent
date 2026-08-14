package cn.forever24.tutor.application.conversation;

import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionStatus;

import java.util.List;

public class ConversationApplicationService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final ConversationReplyStreamer conversationReplyStreamer;
    private final DailyQuotaApplicationService dailyQuotaApplicationService;

    public ConversationApplicationService(
            TrainingSessionRepository trainingSessionRepository,
            LearningPlanRepository learningPlanRepository,
            ConversationReplyStreamer conversationReplyStreamer,
            DailyQuotaApplicationService dailyQuotaApplicationService
    ) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.conversationReplyStreamer = conversationReplyStreamer;
        this.dailyQuotaApplicationService = dailyQuotaApplicationService;
    }

    public List<ConversationStreamEvent> streamMessage(ConversationStreamRequest request) {
        UserKey userKey = new UserKey(request.userKey());
        TrainingSession session = trainingSessionRepository.findById(userKey, request.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("training session was not found"));
        if (session.status() != TrainingSessionStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("training session must be IN_PROGRESS to stream conversation");
        }
        if (request.taskId() != null && !session.currentTaskId().equals(request.taskId())) {
            throw new IllegalArgumentException("task is not the current training task");
        }
        LearningPlan plan = learningPlanRepository.getPlan(userKey, session.planId());
        LearningPlanTask task = plan.tasks().stream()
                .filter(candidate -> candidate.taskId().equals(session.currentTaskId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("current training task was not found"));
        QuotaReservation reservation = dailyQuotaApplicationService.reserve(
                userKey.value(),
                QuotaRequestType.CONVERSATION_REPLY,
                request.idempotencyKey());
        try {
            List<ConversationStreamEvent> events = conversationReplyStreamer.streamReply(
                    new ConversationStreamContext(session, task, request.text()));
            dailyQuotaApplicationService.commit(reservation);
            return events;
        } catch (RuntimeException exception) {
            dailyQuotaApplicationService.refund(reservation);
            throw exception;
        }
    }
}
