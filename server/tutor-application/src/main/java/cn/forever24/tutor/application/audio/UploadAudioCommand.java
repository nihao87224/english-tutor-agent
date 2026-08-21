package cn.forever24.tutor.application.audio;

import java.util.Arrays;

public record UploadAudioCommand(
        String purpose,
        String mimeType,
        long durationMs,
        String claimedSha256,
        byte[] content
) {
    public UploadAudioCommand {
        content = content == null ? null : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return content == null ? null : Arrays.copyOf(content, content.length);
    }
}
