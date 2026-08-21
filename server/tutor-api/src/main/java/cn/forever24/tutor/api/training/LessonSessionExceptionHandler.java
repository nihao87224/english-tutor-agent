package cn.forever24.tutor.api.training;

import cn.forever24.tutor.application.training.LessonSessionApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = {LessonSessionController.class, LessonAttemptController.class})
public class LessonSessionExceptionHandler {

    @ExceptionHandler(LessonSessionApplicationException.class)
    ResponseEntity<ProblemDetail> handleLessonSession(LessonSessionApplicationException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.status());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(exception.code());
        problem.setType(URI.create(
                "https://english-tutor/errors/" + exception.code().toLowerCase().replace('_', '-')));
        problem.setProperty("code", exception.code());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleInvalid(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("VALIDATION_FAILED");
        problem.setProperty("code", "VALIDATION_FAILED");
        return ResponseEntity.badRequest().body(problem);
    }
}
