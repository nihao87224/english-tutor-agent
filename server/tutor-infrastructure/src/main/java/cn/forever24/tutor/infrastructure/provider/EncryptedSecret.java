package cn.forever24.tutor.infrastructure.provider;

public record EncryptedSecret(String ciphertext, String nonce, String keyVersion) {
}
