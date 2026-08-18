package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.planning.LearningPlanApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LearningPlanController {

    private final LearningPlanApplicationService learningPlanApplicationService;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public LearningPlanController(
            LearningPlanApplicationService learningPlanApplicationService,
            CurrentUserKeyResolver currentUserKeyResolver
    ) {
        this.learningPlanApplicationService = learningPlanApplicationService;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @GetMapping("/plans/today")
    public LearningPlanResponse getTodayPlan() {
        return LearningPlanResponse.from(learningPlanApplicationService.getTodayPlan(currentUserKeyResolver.resolve()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid learning plan request");
        return ResponseEntity.badRequest().body(problem);
    }
}
