package com.tlenparty.chat.common.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    @Test
    void serializesAuthCommand() {
        assertEquals("/auth user1 1111", Command.auth("user1", "1111").serialize());
    }

    @Test
    void parsesAuthWithPasswordContainingSpaces() throws ProtocolException {
        Command command = Command.parse("/auth user1 secret with spaces");

        assertEquals(CommandType.AUTH, command.type());
        assertEquals("user1", command.arg(0));
        assertEquals("secret with spaces", command.arg(1));
    }

    @Test
    void keepsMessageTextIntact() throws ProtocolException {
        Command command = Command.parse("/w Morty hello   there, /end");

        assertEquals(CommandType.PRIVATE_MESSAGE, command.type());
        assertEquals("Morty", command.arg(0));
        assertEquals("hello   there, /end", command.arg(1));
    }

    @Test
    void roundTripsEveryFactory() throws ProtocolException {
        List<Command> commands = List.of(
                Command.auth("login", "pass word"),
                Command.authOk("Morty"),
                Command.authError("Логин или пароль неверны"),
                Command.publicMessage("hi all"),
                Command.privateMessage("Morty", "psst"),
                Command.clientMessage("Rick", "wubba lubba"),
                Command.privateDelivery("Rick", "Morty", "secret text"),
                Command.serverMessage("Rick подключился к чату"),
                Command.userList(List.of("Rick", "Morty")),
                Command.userList(List.of()),
                Command.changeUsername("Pickle"),
                Command.changeUsernameOk("Pickle"),
                Command.changeUsernameError("Имя занято"),
                Command.end());

        for (Command command : commands) {
            assertEquals(command, Command.parse(command.serialize()), command.serialize());
        }
    }

    @Test
    void parsesUserList() throws ProtocolException {
        assertEquals(List.of("a", "b", "c"), Command.parse("/userList a b c").args());
        assertEquals(List.of(), Command.parse("/userList").args());
    }

    @Test
    void prefixMatchingIsExact() throws ProtocolException {
        assertEquals(CommandType.AUTH_OK, Command.parse("/authok Rick").type());
        assertThrows(ProtocolException.class, () -> Command.parse("/whisper Rick hi"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "hello", "/unknown arg", "/auth", "/auth onlyLogin", "/w Morty", "/end now", "/msg", "/changeUsername two words"})
    void rejectsMalformedInput(String raw) {
        assertThrows(ProtocolException.class, () -> Command.parse(raw));
    }

    @Test
    void rejectsWhitespaceInTokenArguments() {
        assertThrows(IllegalArgumentException.class, () -> Command.auth("user 1", "1111"));
        assertThrows(IllegalArgumentException.class, () -> Command.changeUsername("two words"));
        assertThrows(IllegalArgumentException.class, () -> Command.userList(List.of("ok", "not ok")));
    }

    @Test
    void detectsTokens() {
        assertTrue(Command.isToken("Morty"));
        assertFalse(Command.isToken(""));
        assertFalse(Command.isToken("a b"));
        assertFalse(Command.isToken(null));
    }
}
