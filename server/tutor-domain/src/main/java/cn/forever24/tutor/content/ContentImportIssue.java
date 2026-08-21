package cn.forever24.tutor.content;

public record ContentImportIssue(String code, String location, String message) {
    public ContentImportIssue {
        if (code == null || code.isBlank() || location == null || location.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("content import issue fields are required");
        }
        code = code.strip();
        location = location.strip();
        message = message.strip();
    }
}
