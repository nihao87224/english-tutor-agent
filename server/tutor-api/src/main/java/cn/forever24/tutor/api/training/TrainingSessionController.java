package cn.forever24.tutor.api.training;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.training.TrainingSessionApplicationService;
import cn.forever24.tutor.training.TaskAttemptInputType;
import cn.forever24.tutor.training.TrainingSessionMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TrainingSessionController {

    private final TrainingSessionApplicationService trainingSessionApplicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public TrainingSessionController(
            TrainingSessionApplicationService trainingSessionApplicationService,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.trainingSessionApplicationService = trainingSessionApplicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @PostMapping("/training-sessions")
    ResponseEntity<TrainingSessionResponse> startTrainingSession(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody StartTrainingSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TrainingSessionResponse.from(
                trainingSessionApplicationService.startDailySession(
                        currentUserKeyResolver.resolve(userKey),
                        request == null ? null : request.planId(),
                        parseMode(request == null ? null : request.mode()),
                        idempotencyKey)));
    }

    @GetMapping("/training-sessions/{sessionId}")
    TrainingSessionResponse getTrainingSession(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("sessionId") String sessionId
    ) {
        return TrainingSessionResponse.from(
                trainingSessionApplicationService.getSession(currentUserKeyResolver.resolve(userKey), sessionId));
    }

    @PostMapping("/training-sessions/{sessionId}/pause")
    TrainingSessionResponse pauseTrainingSession(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("sessionId") String sessionId
    ) {
        return TrainingSessionResponse.from(
                trainingSessionApplicationService.pause(currentUserKeyResolver.resolve(userKey), sessionId));
    }

    @PostMapping("/training-sessions/{sessionId}/resume")
    TrainingSessionResponse resumeTrainingSession(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("sessionId") String sessionId
    ) {
        return TrainingSessionResponse.from(
                trainingSessionApplicationService.resume(currentUserKeyResolver.resolve(userKey), sessionId));
    }

    @PostMapping("/training-sessions/{sessionId}/complete")
    TrainingSessionCompletionResponse completeTrainingSession(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("sessionId") String sessionId
    ) {
        return TrainingSessionCompletionResponse.from(
                trainingSessionApplicationService.complete(currentUserKeyResolver.resolve(userKey), sessionId));
    }

    @GetMapping("/training-sessions/{sessionId}/current-task")
    CurrentTrainingTaskResponse getCurrentTask(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("sessionId") String sessionId
    ) {
        return CurrentTrainingTaskResponse.from(
                trainingSessionApplicationService.getCurrentTask(currentUserKeyResolver.resolve(userKey), sessionId));
    }

    @PostMapping("/training-sessions/{sessionId}/tasks/{taskId}/attempts")
    ResponseEntity<TaskAttemptReceiptResponse> submitTaskAttempt(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("sessionId") String sessionId,
            @PathVariable("taskId") String taskId,
            @RequestBody TaskAttemptRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(TaskAttemptReceiptResponse.from(
                trainingSessionApplicationService.submitTaskAttempt(
                        currentUserKeyResolver.resolve(userKey),
                        sessionId,
                        taskId,
                        parseInputType(request == null ? null : request.inputType()),
                        request == null ? null : request.text(),
                        request == null ? null : request.hintLevel(),
                        request == null ? null : request.clientDurationMs(),
                        request == null ? null : request.clientStartedAt(),
                        request == null ? null : request.clientCompletedAt(),
                        idempotencyKey)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid training session request");
        return ResponseEntity.badRequest().body(problem);
    }

    private TrainingSessionMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return TrainingSessionMode.MIXED;
        }
        return TrainingSessionMode.valueOf(mode);
    }

    private TaskAttemptInputType parseInputType(String inputType) {
        if (inputType == null || inputType.isBlank()) {
            throw new IllegalArgumentException("inputType is required");
        }
        return TaskAttemptInputType.valueOf(inputType);
    }
}
