package cn.forever24.tutor.api.audio;

import cn.forever24.tutor.application.audio.AudioAssetApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = AudioUploadController.class)
public class AudioUploadExceptionHandler {
    @ExceptionHandler(AudioAssetApplicationException.class)
    ResponseEntity<ProblemDetail> handle(AudioAssetApplicationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(exception.status()), exception.getMessage());
        problem.setTitle(exception.code());
        problem.setProperty("code", exception.code());
        return ResponseEntity.status(exception.status()).body(problem);
    }

    @ExceptionHandler({IllegalArgumentException.class, IOException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ProblemDetail> invalid(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("VALIDATION_FAILED");
        problem.setProperty("code", "VALIDATION_FAILED");
        return ResponseEntity.badRequest().body(problem);
    }
}
