package cn.forever24.tutor.application.resource;

public interface ContentObjectStorage {
    StagedObject stage(String finalObjectKey, byte[] content, String expectedContentHash);
    void finalize(StagedObject staged);
    void discard(StagedObject staged);
    void delete(String objectKey);
    boolean exists(String objectKey);
    java.util.List<String> listObjectKeys();

    record StagedObject(String stagingKey, String finalObjectKey, String contentHash) {
        public StagedObject {
            if (stagingKey == null || finalObjectKey == null || contentHash == null) throw new IllegalArgumentException("staged object fields are required");
        }
    }
}
