package cn.forever24.tutor.application.roleplay;

import java.util.List;

public record RolePlayResponseContext(
        String sessionId,
        String turnId,
        String resourceId,
        String resourceVersion,
        String skillUnitVariantId,
        String episodeMappingId,
        RolePlayTask task,
        String learnerText,
        List<RolePlayHistoryTurn> history
) {
    public RolePlayResponseContext {
        if (sessionId == null || sessionId.isBlank() || turnId == null || turnId.isBlank()
                || resourceId == null || resourceId.isBlank() || resourceVersion == null || resourceVersion.isBlank()
                || skillUnitVariantId == null || skillUnitVariantId.isBlank()
                || episodeMappingId == null || episodeMappingId.isBlank()
                || task == null || learnerText == null || learnerText.isBlank()) {
            throw new IllegalArgumentException("role-play response boundary is incomplete");
        }
        history = List.copyOf(history == null ? List.of() : history);
    }
}
