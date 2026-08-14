package cn.forever24.tutor.api.quota;

import cn.forever24.tutor.application.quota.DailyQuotaStatus;
import cn.forever24.tutor.application.quota.QuotaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class QuotaExceptionHandler {

    @ExceptionHandler(QuotaException.class)
    ResponseEntity<ProblemDetail> handleQuota(QuotaException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.status());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(exception.code());
        problem.setType(URI.create("https://english-tutor/errors/" + exception.code().toLowerCase().replace('_', '-')));
        problem.setProperty("code", exception.code());
        DailyQuotaStatus quotaStatus = exception.quotaStatus();
        if (quotaStatus != null) {
            problem.setProperty("dailyLimit", quotaStatus.dailyLimit());
            problem.setProperty("used", quotaStatus.used());
            problem.setProperty("remaining", quotaStatus.remaining());
            problem.setProperty("resetAt", quotaStatus.resetAt());
        }
        return ResponseEntity.status(status).body(problem);
    }
}
