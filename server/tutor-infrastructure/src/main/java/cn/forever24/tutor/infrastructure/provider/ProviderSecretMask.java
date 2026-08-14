package cn.forever24.tutor.infrastructure.provider;

final class ProviderSecretMask {

    private ProviderSecretMask() {
    }

    static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        String trimmed = secret.trim();
        int visibleChars = Math.min(4, trimmed.length());
        return "****" + trimmed.substring(trimmed.length() - visibleChars);
    }
}
