package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.application.planning.PrescriptionApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = PrescriptionController.class)
public class PrescriptionExceptionHandler {

    @ExceptionHandler(PrescriptionApplicationException.class)
    ResponseEntity<ProblemDetail> handlePrescription(PrescriptionApplicationException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.status());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(exception.code());
        problem.setType(URI.create(
                "https://english-tutor/errors/" + exception.code().toLowerCase().replace('_', '-')));
        problem.setProperty("code", exception.code());
        if (exception.fallbackAvailable() != null) {
            problem.setProperty("fallbackAvailable", exception.fallbackAvailable());
        }
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleInvalid(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("INVALID_PRESCRIPTION_REQUEST");
        problem.setProperty("code", "INVALID_PRESCRIPTION_REQUEST");
        return ResponseEntity.badRequest().body(problem);
    }
}
