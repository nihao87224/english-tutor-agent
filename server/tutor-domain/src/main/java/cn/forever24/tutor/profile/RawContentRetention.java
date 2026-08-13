package cn.forever24.tutor.profile;

public enum RawContentRetention {
    STORE,
    PROCESS_ONLY;

    public static RawContentRetention fromSaveFlag(Boolean saveRawContent, String fieldName) {
        if (saveRawContent == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return saveRawContent ? STORE : PROCESS_ONLY;
    }

    public boolean savesRawContent() {
        return this == STORE;
    }
}
