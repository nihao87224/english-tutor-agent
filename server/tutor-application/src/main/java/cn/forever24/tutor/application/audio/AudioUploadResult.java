package cn.forever24.tutor.application.audio;

import cn.forever24.tutor.audio.UserAudioAsset;

public record AudioUploadResult(UserAudioAsset asset, boolean replayed) {
}
