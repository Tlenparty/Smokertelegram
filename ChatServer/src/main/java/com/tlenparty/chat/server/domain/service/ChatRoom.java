package com.tlenparty.chat.server.domain.service;

import com.tlenparty.chat.server.domain.model.Message;
import com.tlenparty.chat.server.domain.model.User;
import com.tlenparty.chat.server.domain.model.UsernamePolicy;
import com.tlenparty.chat.server.domain.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ChatRoom {

    private final UserRepository userRepository;
    private final Map<Participant, User> members = new LinkedHashMap<>();

    public ChatRoom(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public synchronized boolean join(Participant participant, User user) {
        boolean alreadyOnline = members.containsKey(participant) || members.values().stream()
                .anyMatch(member -> member.login().equals(user.login()) || member.username().equals(user.username()));
        if (alreadyOnline) {
            return false;
        }
        members.put(participant, user);
        participant.joined(user);
        forEachExcept(participant, other -> other.userJoined(user.username()));
        publishUserList();
        return true;
    }

    public synchronized void leave(Participant participant) {
        User user = members.remove(participant);
        if (user == null) {
            return;
        }
        forEachExcept(participant, other -> other.userLeft(user.username()));
        publishUserList();
    }

    public synchronized void broadcast(Participant sender, String text) {
        User user = requireMember(sender);
        Message message = Message.broadcast(user.username(), text);
        forEachExcept(sender, other -> other.deliver(message));
    }

    public synchronized PrivateMessageResult sendPrivate(Participant sender, String recipient, String text) {
        User user = requireMember(sender);
        if (user.username().equals(recipient)) {
            return PrivateMessageResult.SELF;
        }
        Optional<Participant> target = findByUsername(recipient);
        if (target.isEmpty()) {
            return PrivateMessageResult.RECIPIENT_OFFLINE;
        }
        target.get().deliver(Message.direct(user.username(), recipient, text));
        return PrivateMessageResult.DELIVERED;
    }

    public synchronized RenameResult rename(Participant participant, String newUsername) {
        User user = requireMember(participant);
        if (!UsernamePolicy.isValid(newUsername)) {
            return RenameResult.INVALID;
        }
        if (user.username().equals(newUsername)) {
            return RenameResult.UNCHANGED;
        }
        if (findByUsername(newUsername).isPresent() || userRepository.existsByUsername(newUsername)) {
            return RenameResult.TAKEN;
        }
        if (!userRepository.updateUsername(user.login(), newUsername)) {
            return RenameResult.ACCOUNT_NOT_FOUND;
        }
        members.put(participant, user.withUsername(newUsername));
        participant.renamed(newUsername);
        forEachExcept(participant, other -> other.userRenamed(user.username(), newUsername));
        publishUserList();
        return RenameResult.RENAMED;
    }

    public synchronized List<String> onlineUsernames() {
        return members.values().stream().map(User::username).toList();
    }

    private User requireMember(Participant participant) {
        User user = members.get(participant);
        if (user == null) {
            throw new IllegalStateException("Participant has not joined the chat");
        }
        return user;
    }

    private Optional<Participant> findByUsername(String username) {
        return members.entrySet().stream()
                .filter(entry -> entry.getValue().username().equals(username))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void publishUserList() {
        List<String> usernames = onlineUsernames();
        members.keySet().forEach(member -> member.usersOnline(usernames));
    }

    private void forEachExcept(Participant excluded, Consumer<Participant> action) {
        members.keySet().stream().filter(member -> member != excluded).forEach(action);
    }
}
