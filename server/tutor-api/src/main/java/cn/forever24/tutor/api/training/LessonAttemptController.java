package cn.forever24.tutor.api.training;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.training.LessonAttemptApplicationService;
import cn.forever24.tutor.training.LessonAttemptStatus;
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
@RequestMapping("/api/v1/lesson-sessions/{sessionId}/attempts")
public class LessonAttemptController {
    private final LessonAttemptApplicationService service;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public LessonAttemptController(
            LessonAttemptApplicationService service,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.service = service;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @PostMapping
    public ResponseEntity<LessonAttemptResponse> submit(
            @PathVariable String sessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody SubmitLessonAttemptRequest request
    ) {
        var result = service.submit(
                currentUserKeyResolver.resolve(), sessionId, request.toCommand(), idempotencyKey);
        HttpStatus status = result.attempt().status() == LessonAttemptStatus.ANALYSIS_PENDING
                ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(LessonAttemptResponse.from(result));
    }

    @GetMapping("/{attemptId}")
    public LessonAttemptResponse get(
            @PathVariable String sessionId,
            @PathVariable String attemptId
    ) {
        return LessonAttemptResponse.from(service.get(
                currentUserKeyResolver.resolve(), sessionId, attemptId));
    }

    @PostMapping("/{attemptId}/transcript-confirmations")
    public ResponseEntity<LessonAttemptResponse> confirmTranscript(
            @PathVariable String sessionId,
            @PathVariable String attemptId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TranscriptConfirmationRequest request
    ) {
        var result = service.confirmTranscript(currentUserKeyResolver.resolve(), sessionId, attemptId,
                request.toCommand(), idempotencyKey);
        return ResponseEntity.ok().header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(LessonAttemptResponse.from(result));
    }
}
