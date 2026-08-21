package cn.forever24.tutor.application.roleplay;

import java.util.List;

public record RolePlayResponse(
        List<String> chunks,
        String promptVersion,
        String providerId,
        String modelId,
        String traceId
) {
    public RolePlayResponse {
        chunks = List.copyOf(chunks == null ? List.of() : chunks);
        if (chunks.isEmpty() || chunks.stream().anyMatch(chunk -> chunk == null || chunk.isBlank())) {
            throw new RolePlayResponderException("AI_OUTPUT_INVALID", false, "role-play reply chunks are invalid");
        }
        String combined = String.join("", chunks);
        if (combined.isBlank() || combined.length() > 2000 || combined.chars().anyMatch(value -> value == 0)) {
            throw new RolePlayResponderException("AI_OUTPUT_INVALID", false, "role-play reply is invalid");
        }
        require(promptVersion, "promptVersion");
        require(providerId, "providerId");
        require(modelId, "modelId");
        require(traceId, "traceId");
    }

    public String text() {
        return String.join("", chunks);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RolePlayResponderException("AI_OUTPUT_INVALID", false, field + " is missing");
        }
    }
}
