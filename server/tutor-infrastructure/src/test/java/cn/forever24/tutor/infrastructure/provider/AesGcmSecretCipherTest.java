package cn.forever24.tutor.infrastructure.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AesGcmSecretCipherTest {

    @Test
    void encryptsAndDecryptsProviderSecret() {
        AesGcmSecretCipher cipher = new AesGcmSecretCipher("0123456789abcdef0123456789abcdef", "test");

        EncryptedSecret encrypted = cipher.encrypt("sk-real-secret");

        assertNotEquals("sk-real-secret", encrypted.ciphertext());
        assertEquals("test", encrypted.keyVersion());
        assertEquals("sk-real-secret", cipher.decrypt(encrypted));
    }
}
