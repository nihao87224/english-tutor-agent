package cn.forever24.tutor.api.training;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.training.LessonSessionApplicationService;
import cn.forever24.tutor.application.training.LessonSessionMutationResult;
import cn.forever24.tutor.application.training.LessonAttemptApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lesson-sessions")
public class LessonSessionController {

    private final LessonSessionApplicationService service;
    private final CurrentUserKeyResolver currentUserKeyResolver;
    private final LessonAttemptApplicationService attemptService;

    public LessonSessionController(
            LessonSessionApplicationService service,
            LessonAttemptApplicationService attemptService,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.service = service;
        this.attemptService = attemptService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @PostMapping
    public ResponseEntity<LessonSessionResponse> start(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody StartLessonSessionRequest request
    ) {
        LessonSessionMutationResult result = service.start(
                currentUserKeyResolver.resolve(), request.toCommand(), idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(LessonSessionResponse.from(result.session()));
    }

    @GetMapping("/{sessionId}")
    public LessonSessionResponse get(@PathVariable String sessionId) {
        String userKey = currentUserKeyResolver.resolve();
        var session = service.get(userKey, sessionId);
        return LessonSessionResponse.from(session, attemptService.progress(userKey, session));
    }

    @PostMapping("/{sessionId}/pause")
    public LessonSessionResponse pause(
            @PathVariable String sessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return LessonSessionResponse.from(
                service.pause(currentUserKeyResolver.resolve(), sessionId, idempotencyKey));
    }

    @PostMapping("/{sessionId}/resume")
    public LessonSessionResponse resume(
            @PathVariable String sessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return LessonSessionResponse.from(
                service.resume(currentUserKeyResolver.resolve(), sessionId, idempotencyKey));
    }

    @PostMapping("/{sessionId}/steps/{stepId}/completions")
    public LessonSessionResponse completeStep(
            @PathVariable String sessionId,
            @PathVariable String stepId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return LessonSessionResponse.from(service.completeStep(
                currentUserKeyResolver.resolve(), sessionId, stepId, idempotencyKey));
    }
}
