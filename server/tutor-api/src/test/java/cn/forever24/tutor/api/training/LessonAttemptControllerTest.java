package cn.forever24.tutor.api.training;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.training.LessonAttemptApplicationService;
import cn.forever24.tutor.application.training.LessonAttemptMutationResult;
import cn.forever24.tutor.application.training.LessonAttemptProgress;
import cn.forever24.tutor.application.training.TranscriptConfirmationDecision;
import cn.forever24.tutor.application.training.SubmitLessonAttemptCommand;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonObjectiveResult;
import cn.forever24.tutor.training.LessonStep;
import cn.forever24.tutor.training.TaskAttemptInputType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LessonAttemptControllerTest {
    @Test
    void returnsAnalyzedObjectiveAttemptAsOkAndPendingSpeakingAsAccepted() {
        LessonAttemptApplicationService service = mock(LessonAttemptApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr-1");
        LessonAttemptController controller = new LessonAttemptController(service, resolver);
        SubmitLessonAttemptRequest objectiveRequest = new SubmitLessonAttemptRequest(
                "q1", TaskAttemptInputType.TEXT, "Gate 24", null, null, null, null);
        when(service.submit("usr-1", "lsn-1", objectiveRequest.toCommand(), "idem-1"))
                .thenReturn(result(LessonAttemptStatus.ANALYZED, true));

        var objective = controller.submit("lsn-1", "idem-1", objectiveRequest);
        assertEquals(200, objective.getStatusCode().value());
        assertEquals(true, objective.getBody().objectiveResult().correct());
        assertEquals("false", objective.getHeaders().getFirst("Idempotency-Replayed"));

        SubmitLessonAttemptRequest speakingRequest = new SubmitLessonAttemptRequest(
                "guided-1", TaskAttemptInputType.TEXT, "Gate 24.", null, null, null, null);
        when(service.submit("usr-1", "lsn-1", speakingRequest.toCommand(), "idem-2"))
                .thenReturn(result(LessonAttemptStatus.ANALYSIS_PENDING, false));
        assertEquals(202, controller.submit("lsn-1", "idem-2", speakingRequest).getStatusCode().value());

        when(service.get("usr-1", "lsn-1", "lat-1"))
                .thenReturn(result(LessonAttemptStatus.ANALYSIS_RETRYABLE, false));
        assertEquals("ANALYSIS_RETRYABLE", controller.get("lsn-1", "lat-1").status());
    }

    @Test
    void exposesLowConfidenceTranscriptAndConfirmationEndpoint() {
        LessonAttemptApplicationService service = mock(LessonAttemptApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr-1");
        LessonAttemptController controller = new LessonAttemptController(service, resolver);
        var low = audioResult(LessonAttemptStatus.RECEIVED, false);
        var request = new SubmitLessonAttemptRequest(
                "guided-1", TaskAttemptInputType.AUDIO, null, "usr_audio_1", null, null, 900);
        when(service.submit("usr-1", "lsn-1", request.toCommand(), "audio-key")).thenReturn(low);

        var submitted = controller.submit("lsn-1", "audio-key", request);
        assertEquals(200, submitted.getStatusCode().value());
        assertEquals(true, submitted.getBody().transcript().confirmationRequired());
        assertEquals(0.42, submitted.getBody().transcript().confidence());

        var confirmation = new TranscriptConfirmationRequest(TranscriptConfirmationDecision.CONFIRM, null);
        when(service.confirmTranscript("usr-1", "lsn-1", "lat-audio", confirmation.toCommand(), "confirm-key"))
                .thenReturn(audioResult(LessonAttemptStatus.ANALYSIS_PENDING, true));
        var confirmed = controller.confirmTranscript("lsn-1", "lat-audio", "confirm-key", confirmation);
        assertEquals(200, confirmed.getStatusCode().value());
        assertEquals(false, confirmed.getBody().transcript().confirmationRequired());
    }

    private static LessonAttemptMutationResult result(LessonAttemptStatus status, boolean objective) {
        LessonAttempt attempt = new LessonAttempt(
                "lat-1", "lsn-1", objective ? "q1" : "guided-1", TaskAttemptInputType.TEXT, "answer",
                null, null, null, false, status,
                objective ? new LessonObjectiveResult(true, "Gate 24", "Answer confirmed.") : null,
                Instant.parse("2026-08-21T01:00:00Z"), 1);
        return new LessonAttemptMutationResult(
                attempt,
                new LessonAttemptProgress(
                        objective ? LessonStep.COMPREHENSION : LessonStep.ROLE_PLAY,
                        objective ? List.of("q1") : List.of(), List.of(), true,
                        objective ? null : "lat-1"),
                false);
    }

    private static LessonAttemptMutationResult audioResult(LessonAttemptStatus status, boolean confirmed) {
        LessonAttempt attempt = new LessonAttempt(
                "lat-audio", "lsn-1", "guided-1", TaskAttemptInputType.AUDIO, null,
                "usr_audio_1", "Gate twenty four", 0.42, confirmed, status, null,
                Instant.parse("2026-08-21T01:00:00Z"), confirmed ? 3 : 2);
        return new LessonAttemptMutationResult(attempt,
                new LessonAttemptProgress(confirmed ? LessonStep.ROLE_PLAY : LessonStep.GUIDED_SPEAKING,
                        List.of(), List.of(), confirmed, confirmed ? null : "lat-audio"), false);
    }
}
