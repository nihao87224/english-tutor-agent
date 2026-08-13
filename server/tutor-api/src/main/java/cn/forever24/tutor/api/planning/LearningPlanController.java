package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.application.planning.LearningPlanApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LearningPlanController {

    private static final String DEFAULT_USER_KEY = "local-dev-user";

    private final LearningPlanApplicationService learningPlanApplicationService;

    public LearningPlanController(LearningPlanApplicationService learningPlanApplicationService) {
        this.learningPlanApplicationService = learningPlanApplicationService;
    }

    @GetMapping("/plans/today")
    public LearningPlanResponse getTodayPlan(
            @RequestHeader(name = "X-User-Key", required = false) String userKey
    ) {
        return LearningPlanResponse.from(learningPlanApplicationService.getTodayPlan(resolveUserKey(userKey)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid learning plan request");
        return ResponseEntity.badRequest().body(problem);
    }

    private String resolveUserKey(String userKey) {
        if (userKey == null || userKey.isBlank()) {
            return DEFAULT_USER_KEY;
        }
        return userKey;
    }
}
