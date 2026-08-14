package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.assessment.AssessmentApplicationService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AssessmentController {

    private final AssessmentApplicationService assessmentApplicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public AssessmentController(
            AssessmentApplicationService assessmentApplicationService,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.assessmentApplicationService = assessmentApplicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentSessionResponse startAssessment(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestBody(required = false) StartAssessmentRequest request
    ) {
        Integer targetMinutes = request == null ? null : request.targetMinutes();
        return AssessmentSessionResponse.from(assessmentApplicationService.startInitialAssessment(
                currentUserKeyResolver.resolve(userKey),
                targetMinutes));
    }

    @PostMapping("/assessments/{assessmentId}/answers")
    public AnswerReceiptResponse submitAssessmentAnswer(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("assessmentId") String assessmentId,
            @RequestBody AssessmentAnswerRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return AnswerReceiptResponse.from(assessmentApplicationService.submitAssessmentAnswer(
                currentUserKeyResolver.resolve(userKey),
                assessmentId,
                request.itemId(),
                request.answerType(),
                request.option(),
                request.text(),
                request.clientDurationMs()));
    }

    @PostMapping("/assessments/{assessmentId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AssessmentCompletionResponse completeAssessment(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("assessmentId") String assessmentId
    ) {
        return AssessmentCompletionResponse.from(assessmentApplicationService.completeAssessment(
                currentUserKeyResolver.resolve(userKey),
                assessmentId));
    }

    @GetMapping("/assessments/{assessmentId}/result")
    public AssessmentResultResponse getAssessmentResult(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @PathVariable("assessmentId") String assessmentId
    ) {
        return AssessmentResultResponse.from(assessmentApplicationService.getAssessmentResult(
                currentUserKeyResolver.resolve(userKey),
                assessmentId));
    }

    @PostMapping("/assessments/self")
    @ResponseStatus(HttpStatus.CREATED)
    public SelfAssessmentResponse submitSelfAssessment(
            @RequestHeader(name = "X-User-Key", required = false) String userKey,
            @RequestBody SelfAssessmentRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        return SelfAssessmentResponse.from(assessmentApplicationService.submitSelfAssessment(
                currentUserKeyResolver.resolve(userKey),
                request.listening(),
                request.speaking(),
                request.reading(),
                request.writing()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid assessment request");
        return ResponseEntity.badRequest().body(problem);
    }
}
