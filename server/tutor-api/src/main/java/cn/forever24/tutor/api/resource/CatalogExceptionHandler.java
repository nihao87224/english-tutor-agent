package cn.forever24.tutor.api.resource;

import cn.forever24.tutor.application.resource.CatalogApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = {LearningResourceController.class, AdminCatalogController.class})
public class CatalogExceptionHandler {

    @ExceptionHandler(CatalogApplicationException.class)
    ResponseEntity<ProblemDetail> handleCatalog(CatalogApplicationException exception) {
        return problem(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleInvalid(IllegalArgumentException exception) {
        return problem(400, "INVALID_REQUEST", exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(int statusCode, String code, String detail) {
        HttpStatus status = HttpStatus.valueOf(statusCode);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code);
        problem.setType(URI.create("https://english-tutor/errors/" + code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
