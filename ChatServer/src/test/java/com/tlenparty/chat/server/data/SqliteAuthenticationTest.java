package com.tlenparty.chat.server.data;

import com.tlenparty.chat.server.domain.model.User;
import com.tlenparty.chat.server.domain.service.PasswordAuthService;
import com.tlenparty.chat.server.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteAuthenticationTest {

    @TempDir
    Path tempDir;

    private SqliteUserRepository repository;
    private PasswordAuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        repository = new SqliteUserRepository(TestDatabase.jdbcUrl(TestDatabase.create(tempDir)));
        authService = new PasswordAuthService(repository, new Pbkdf2PasswordHasher());
    }

    @Test
    void authenticatesWithValidCredentials() {
        assertEquals(Optional.of(new User("user1", "Rick")), authService.authenticate("user1", "1111"));
    }

    @Test
    void rejectsWrongPassword() {
        assertTrue(authService.authenticate("user1", "2222").isEmpty());
    }

    @Test
    void rejectsUnknownLogin() {
        assertTrue(authService.authenticate("ghost", "1111").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"' OR '1'='1", "user1' --", "user1'; DROP TABLE auth; --", "' UNION SELECT 'x','y','z' --"})
    void sqlInjectionInLoginDoesNotAuthenticate(String login) {
        assertTrue(authService.authenticate(login, "1111").isEmpty());
        assertTrue(authService.authenticate(login, "anything").isEmpty());
        assertTrue(repository.findByLogin("user1").isPresent());
    }

    @Test
    void storesPasswordsHashed() {
        String stored = repository.findByLogin("user1").orElseThrow().passwordHash();
        assertFalse(stored.contains("1111"));
        assertTrue(stored.startsWith("pbkdf2-sha256$"));
    }

    @Test
    void updatesUsernameByLogin() {
        assertTrue(repository.updateUsername("user1", "Pickle"));

        assertTrue(repository.existsByUsername("Pickle"));
        assertFalse(repository.existsByUsername("Rick"));
        assertEquals(Optional.of(new User("user1", "Pickle")), authService.authenticate("user1", "1111"));
        assertFalse(repository.updateUsername("ghost", "Nobody"));
    }
}
