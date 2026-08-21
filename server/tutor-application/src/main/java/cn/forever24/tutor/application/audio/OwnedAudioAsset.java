package cn.forever24.tutor.application.audio;

import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.UserKey;

public record OwnedAudioAsset(UserKey owner, UserAudioAsset asset) { }
