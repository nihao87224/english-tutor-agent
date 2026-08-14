package cn.forever24.tutor.api.provider;

import cn.forever24.tutor.application.provider.AiProviderConfigurationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class AiProviderExceptionHandler {

    @ExceptionHandler(AiProviderConfigurationException.class)
    ResponseEntity<ProblemDetail> handleAiProviderConfiguration(AiProviderConfigurationException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.status());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(exception.code());
        problem.setType(URI.create("https://english-tutor/errors/" + exception.code().toLowerCase().replace('_', '-')));
        problem.setProperty("code", exception.code());
        return ResponseEntity.status(status).body(problem);
    }
}
