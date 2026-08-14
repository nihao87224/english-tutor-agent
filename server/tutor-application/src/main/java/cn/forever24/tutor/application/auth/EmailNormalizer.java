package cn.forever24.tutor.application.auth;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            throw AuthException.badRequest("INVALID_EMAIL", "Email is invalid");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 320 || !normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw AuthException.badRequest("INVALID_EMAIL", "Email is invalid");
        }
        return normalized;
    }
}
