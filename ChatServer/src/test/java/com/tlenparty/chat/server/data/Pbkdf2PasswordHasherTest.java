package com.tlenparty.chat.server.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

    @Test
    void matchesOwnHashOnly() {
        String hash = hasher.hash("1111");

        assertTrue(hasher.matches("1111", hash));
        assertFalse(hasher.matches("1112", hash));
    }

    @Test
    void usesRandomSalt() {
        assertNotEquals(hasher.hash("1111"), hasher.hash("1111"));
    }

    @Test
    void rejectsMalformedOrPlainTextHashes() {
        assertFalse(hasher.matches("1111", "1111"));
        assertFalse(hasher.matches("1111", "pbkdf2-sha256$x$y$z"));
        assertFalse(hasher.matches("1111", null));
    }
}
