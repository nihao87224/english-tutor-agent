package cn.forever24.tutor.application.roleplay;

public record RolePlayHistoryTurn(String learnerText, String replyText) {
    public RolePlayHistoryTurn {
        if (learnerText == null || learnerText.isBlank() || replyText == null || replyText.isBlank()) {
            throw new IllegalArgumentException("completed role-play history requires learner and reply text");
        }
    }
}
