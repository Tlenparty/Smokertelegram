package com.tlenparty.chat.server.domain.service;

import com.tlenparty.chat.server.domain.model.Message;
import com.tlenparty.chat.server.domain.model.User;
import com.tlenparty.chat.server.domain.model.UserAccount;
import com.tlenparty.chat.server.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRoomTest {

    private InMemoryUserRepository repository;
    private ChatRoom room;
    private FakeParticipant rick;
    private FakeParticipant morty;
    private FakeParticipant summer;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        repository.add("user1", "Rick");
        repository.add("user2", "Morty");
        repository.add("user3", "Summer");
        room = new ChatRoom(repository);
        rick = new FakeParticipant();
        morty = new FakeParticipant();
        summer = new FakeParticipant();
        assertTrue(room.join(rick, new User("user1", "Rick")));
        assertTrue(room.join(morty, new User("user2", "Morty")));
        assertTrue(room.join(summer, new User("user3", "Summer")));
    }

    @Test
    void joinConfirmsUserAndPublishesListToEveryone() {
        assertEquals("joined:Rick", rick.events.get(0));
        assertTrue(rick.events.contains("userJoined:Morty"));
        assertFalse(morty.events.contains("userJoined:Morty"));
        assertEquals(List.of("Rick", "Morty", "Summer"), summer.lastUserList);
        assertEquals(List.of("Rick", "Morty", "Summer"), rick.lastUserList);
    }

    @Test
    void rejectsSecondSessionForSameAccount() {
        FakeParticipant duplicate = new FakeParticipant();

        assertFalse(room.join(duplicate, new User("user1", "Rick")));
        assertTrue(duplicate.events.isEmpty());
        assertEquals(List.of("Rick", "Morty", "Summer"), room.onlineUsernames());
    }

    @Test
    void broadcastReachesEveryoneExceptSender() {
        room.broadcast(rick, "hello");

        assertTrue(rick.messages.isEmpty());
        assertEquals(List.of(Message.broadcast("Rick", "hello")), morty.messages);
        assertEquals(List.of(Message.broadcast("Rick", "hello")), summer.messages);
    }

    @Test
    void privateMessageReachesOnlyRecipient() {
        assertEquals(PrivateMessageResult.DELIVERED, room.sendPrivate(rick, "Morty", "psst"));

        assertEquals(List.of(Message.direct("Rick", "Morty", "psst")), morty.messages);
        assertTrue(summer.messages.isEmpty());
        assertTrue(rick.messages.isEmpty());
    }

    @Test
    void privateMessageToOfflineOrSelfIsNotDelivered() {
        assertEquals(PrivateMessageResult.RECIPIENT_OFFLINE, room.sendPrivate(rick, "Jerry", "hi"));
        assertEquals(PrivateMessageResult.SELF, room.sendPrivate(rick, "Rick", "hi"));
        assertTrue(rick.messages.isEmpty());
        assertTrue(morty.messages.isEmpty());
    }

    @Test
    void renamePersistsAndNotifiesOthers() {
        assertEquals(RenameResult.RENAMED, room.rename(rick, "Pickle"));

        assertEquals("Pickle", repository.usernames.get("user1"));
        assertTrue(rick.events.contains("renamed:Pickle"));
        assertTrue(morty.events.contains("userRenamed:Rick->Pickle"));
        assertEquals(List.of("Pickle", "Morty", "Summer"), morty.lastUserList);

        room.sendPrivate(morty, "Pickle", "nice");
        assertEquals(List.of(Message.direct("Morty", "Pickle", "nice")), rick.messages);
    }

    @Test
    void renameRejectsTakenUnchangedAndInvalidNames() {
        repository.add("user4", "Jerry");

        assertEquals(RenameResult.TAKEN, room.rename(rick, "Morty"));
        assertEquals(RenameResult.TAKEN, room.rename(rick, "Jerry"));
        assertEquals(RenameResult.UNCHANGED, room.rename(rick, "Rick"));
        assertEquals(RenameResult.INVALID, room.rename(rick, "two words"));
        assertEquals(RenameResult.INVALID, room.rename(rick, "x".repeat(33)));
        assertEquals("Rick", repository.usernames.get("user1"));
    }

    @Test
    void leaveNotifiesOthersAndUpdatesList() {
        room.leave(morty);

        assertTrue(rick.events.contains("userLeft:Morty"));
        assertEquals(List.of("Rick", "Summer"), rick.lastUserList);
        assertEquals(PrivateMessageResult.RECIPIENT_OFFLINE, room.sendPrivate(rick, "Morty", "hi"));

        room.leave(morty);
        assertEquals(List.of("Rick", "Summer"), room.onlineUsernames());
    }

    private static final class FakeParticipant implements Participant {
        final List<String> events = new ArrayList<>();
        final List<Message> messages = new ArrayList<>();
        List<String> lastUserList = List.of();

        @Override
        public void joined(User user) {
            events.add("joined:" + user.username());
        }

        @Override
        public void renamed(String newUsername) {
            events.add("renamed:" + newUsername);
        }

        @Override
        public void deliver(Message message) {
            messages.add(message);
        }

        @Override
        public void userJoined(String username) {
            events.add("userJoined:" + username);
        }

        @Override
        public void userLeft(String username) {
            events.add("userLeft:" + username);
        }

        @Override
        public void userRenamed(String oldUsername, String newUsername) {
            events.add("userRenamed:" + oldUsername + "->" + newUsername);
        }

        @Override
        public void usersOnline(List<String> usernames) {
            lastUserList = usernames;
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {
        final Map<String, String> usernames = new HashMap<>();

        void add(String login, String username) {
            usernames.put(login, username);
        }

        @Override
        public Optional<UserAccount> findByLogin(String login) {
            return Optional.ofNullable(usernames.get(login)).map(name -> new UserAccount(login, "", name));
        }

        @Override
        public boolean existsByUsername(String username) {
            return usernames.containsValue(username);
        }

        @Override
        public boolean updateUsername(String login, String newUsername) {
            return usernames.replace(login, newUsername) != null;
        }
    }
}
