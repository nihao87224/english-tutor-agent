package cn.forever24.tutor.application.audio;

import cn.forever24.tutor.audio.UserAudioAsset;

public record AudioAssetStoreRecord(String requestHash, UserAudioAsset asset) {
}
