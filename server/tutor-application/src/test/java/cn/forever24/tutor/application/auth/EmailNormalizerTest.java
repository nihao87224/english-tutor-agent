package cn.forever24.tutor.application.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailNormalizerTest {

    @Test
    void normalizesTrimmedLowercaseEmail() {
        assertEquals("steven@example.com", EmailNormalizer.normalize(" Steven@Example.COM "));
    }

    @Test
    void rejectsInvalidEmail() {
        assertThrows(AuthException.class, () -> EmailNormalizer.normalize("not-an-email"));
    }
}
