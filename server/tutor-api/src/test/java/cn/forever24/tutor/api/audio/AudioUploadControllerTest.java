package cn.forever24.tutor.api.audio;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.audio.AudioUploadApplicationService;
import cn.forever24.tutor.application.audio.AudioUploadResult;
import cn.forever24.tutor.application.audio.UploadAudioCommand;
import cn.forever24.tutor.audio.AudioAssetStatus;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.RawContentRetention;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioUploadControllerTest {
    @Test
    void returnsReadyProtectedAssetAndReplayHeader() throws Exception {
        AudioUploadApplicationService service = mock(AudioUploadApplicationService.class);
        CurrentUserKeyResolver resolver = mock(CurrentUserKeyResolver.class);
        MultipartFile file = mock(MultipartFile.class);
        when(resolver.resolve()).thenReturn("usr-1");
        when(file.getContentType()).thenReturn("audio/webm");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(service.upload(org.mockito.ArgumentMatchers.eq("usr-1"), any(UploadAudioCommand.class),
                org.mockito.ArgumentMatchers.eq("key")))
                .thenReturn(new AudioUploadResult(asset(), false));

        var response = new AudioUploadController(service, resolver)
                .upload("key", file, 1_000, "LESSON_ATTEMPT", null);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("READY", response.getBody().uploadStatus());
        assertEquals("usr_audio_1", response.getBody().audioAssetId());
        assertEquals("false", response.getHeaders().getFirst("Idempotency-Replayed"));
    }

    private static UserAudioAsset asset() {
        return new UserAudioAsset("usr_audio_1", "private/1.webm", "LESSON_ATTEMPT", "audio/webm",
                3, 1_000, "sha256:abc", AudioAssetStatus.READY, RawContentRetention.STORE, null,
                Instant.parse("2026-08-21T01:00:00Z"));
    }
}
