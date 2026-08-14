package cn.forever24.tutor.infrastructure.provider;

import cn.forever24.tutor.application.provider.AiProviderConfigurationException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class AesGcmSecretCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final byte[] key;
    private final String keyVersion;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSecretCipher(String rawKey, String keyVersion) {
        this.key = normalizeKey(rawKey);
        this.keyVersion = keyVersion == null || keyVersion.isBlank() ? "v1" : keyVersion.trim();
    }

    public EncryptedSecret encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw AiProviderConfigurationException.invalid("secret plaintext is required");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedSecret(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(nonce),
                    keyVersion);
        } catch (GeneralSecurityException exception) {
            throw AiProviderConfigurationException.unavailable("Failed to encrypt provider secret");
        }
    }

    public String decrypt(EncryptedSecret encryptedSecret) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] nonce = Base64.getDecoder().decode(encryptedSecret.nonce());
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(encryptedSecret.ciphertext()));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw AiProviderConfigurationException.unavailable("Failed to decrypt provider secret");
        }
    }

    private static byte[] normalizeKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw AiProviderConfigurationException.unavailable("TUTOR_SECRET_ENCRYPTION_KEY must be configured");
        }
        String trimmed = rawKey.trim();
        byte[] raw = trimmed.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 32) {
            return raw;
        }
        byte[] decoded = tryDecode(trimmed);
        if (decoded != null && decoded.length == 32) {
            return decoded;
        }
        throw AiProviderConfigurationException.unavailable("TUTOR_SECRET_ENCRYPTION_KEY must be 32 bytes or base64 encoded 32 bytes");
    }

    private static byte[] tryDecode(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
