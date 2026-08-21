package cn.forever24.tutor.application.audio;

public interface PrivateAudioObjectStorage {
    void put(String objectKey, byte[] content);
    byte[] read(String objectKey);
    void delete(String objectKey);
}
