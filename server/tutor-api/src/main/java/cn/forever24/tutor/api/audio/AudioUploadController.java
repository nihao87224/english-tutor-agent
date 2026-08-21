package cn.forever24.tutor.api.audio;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.audio.AudioUploadApplicationService;
import cn.forever24.tutor.application.audio.UploadAudioCommand;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/audio/uploads")
public class AudioUploadController {
    private final AudioUploadApplicationService service;
    private final CurrentUserKeyResolver currentUserKeyResolver;

    public AudioUploadController(AudioUploadApplicationService service, CurrentUserKeyResolver currentUserKeyResolver) {
        this.service = service;
        this.currentUserKeyResolver = currentUserKeyResolver;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioUploadResponse> upload(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam("file") MultipartFile file,
            @RequestParam long durationMs,
            @RequestParam(defaultValue = "LESSON_ATTEMPT") String purpose,
            @RequestParam(required = false) String sha256
    ) throws IOException {
        var result = service.upload(currentUserKeyResolver.resolve(), new UploadAudioCommand(
                purpose, file.getContentType(), durationMs, sha256, file.getBytes()), idempotencyKey);
        return ResponseEntity.status(result.replayed() ? 200 : 201)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(AudioUploadResponse.from(result));
    }
}
