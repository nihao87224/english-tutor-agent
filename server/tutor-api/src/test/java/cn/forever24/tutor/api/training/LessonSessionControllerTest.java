package cn.forever24.tutor.api.training;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.training.LessonSessionApplicationException;
import cn.forever24.tutor.application.training.LessonSessionApplicationService;
import cn.forever24.tutor.application.training.LessonAttemptApplicationService;
import cn.forever24.tutor.application.training.LessonSessionMutationResult;
import cn.forever24.tutor.application.training.StartLessonSessionCommand;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonStep;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonSessionControllerTest {

    @Test
    void startReturnsReplayStatusHeaderAndExactResourceVersion() {
        LessonSessionApplicationService service = mock(LessonSessionApplicationService.class);
        LessonAttemptApplicationService attemptService = mock(LessonAttemptApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        when(resolver.resolve()).thenReturn("usr-1");
        StartLessonSessionRequest request = new StartLessonSessionRequest(
                "prx-1", 3, "blk-1", LessonInputMode.VOICE_OR_TEXT);
        when(service.start("usr-1", request.toCommand(), "idem-1"))
                .thenReturn(new LessonSessionMutationResult(session(), true));
        LessonSessionController controller = new LessonSessionController(service, attemptService, resolver);

        ResponseEntity<LessonSessionResponse> response = controller.start("idem-1", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("true", response.getHeaders().getFirst("Idempotency-Replayed"));
        assertEquals("1.0.0", response.getBody().resource().resourceVersion());
        assertEquals(LessonStep.SCENE_CONTEXT.name(), response.getBody().step().stepId());
        assertTrue(response.getBody().step().clientCompletable());
        verify(service).start("usr-1", new StartLessonSessionCommand(
                "prx-1", 3, "blk-1", LessonInputMode.VOICE_OR_TEXT), "idem-1");
    }

    @Test
    void routesExposeGetPauseResumeAndStepCompletion() throws Exception {
        assertEquals("/{sessionId}", LessonSessionController.class
                .getMethod("get", String.class).getAnnotation(GetMapping.class).value()[0]);
        assertEquals("/{sessionId}/pause", LessonSessionController.class
                .getMethod("pause", String.class, String.class).getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/{sessionId}/resume", LessonSessionController.class
                .getMethod("resume", String.class, String.class).getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/{sessionId}/steps/{stepId}/completions", LessonSessionController.class
                .getMethod("completeStep", String.class, String.class, String.class)
                .getAnnotation(PostMapping.class).value()[0]);
    }

    @Test
    void stableSessionConflictIsMappedToProblemDetail() {
        LessonSessionExceptionHandler handler = new LessonSessionExceptionHandler();

        ResponseEntity<?> response = handler.handleLessonSession(
                LessonSessionApplicationException.stateConflict("step requires an attempt"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("SESSION_STATE_CONFLICT", response.getBody().toString().contains("SESSION_STATE_CONFLICT")
                ? "SESSION_STATE_CONFLICT" : "missing");
    }

    private static LessonSession session() {
        return LessonSession.start(
                "lsn-1", "prx-1", 3, "blk-1", "resource-1", "1.0.0",
                "skill.a2", "mapping-1", TrainingType.ROLE_PLAY,
                LessonInputMode.VOICE_OR_TEXT, Instant.parse("2026-08-20T02:00:00Z"));
    }
}
