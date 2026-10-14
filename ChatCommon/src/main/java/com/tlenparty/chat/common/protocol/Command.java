package com.tlenparty.chat.common.protocol;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record Command(CommandType type, List<String> args) {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public Command {
        Objects.requireNonNull(type, "type");
        args = List.copyOf(args);
        if (type.isVariadic()) {
            args.forEach(arg -> requireToken(type, arg));
        } else {
            if (args.size() != type.arity()) {
                throw new IllegalArgumentException(type.prefix() + " expects " + type.arity() + " argument(s), got " + args.size());
            }
            for (int i = 0; i < args.size(); i++) {
                boolean freeText = type.hasFreeTextTail() && i == args.size() - 1;
                if (!freeText) {
                    requireToken(type, args.get(i));
                } else if (args.get(i).isBlank()) {
                    throw new IllegalArgumentException(type.prefix() + " requires non-blank text");
                }
            }
        }
    }

    public Command(CommandType type, String... args) {
        this(type, Arrays.asList(args));
    }

    public String arg(int index) {
        return args.get(index);
    }

    public static boolean isToken(String value) {
        return value != null && !value.isEmpty() && !WHITESPACE.matcher(value).find();
    }

    public String serialize() {
        return args.isEmpty() ? type.prefix() : type.prefix() + " " + String.join(" ", args);
    }

    public static Command parse(String raw) throws ProtocolException {
        if (raw == null || raw.isBlank()) {
            throw new ProtocolException("Empty command");
        }
        String[] head = WHITESPACE.split(raw.strip(), 2);
        CommandType type = CommandType.fromPrefix(head[0])
                .orElseThrow(() -> new ProtocolException("Unknown command: " + head[0]));
        String rest = head.length > 1 ? head[1] : "";
        try {
            return new Command(type, splitArgs(type, rest));
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Malformed command " + type.prefix() + ": " + e.getMessage(), e);
        }
    }

    private static List<String> splitArgs(CommandType type, String rest) {
        if (rest.isEmpty()) {
            return List.of();
        }
        if (type.isVariadic() || !type.hasFreeTextTail()) {
            return Arrays.asList(WHITESPACE.split(rest));
        }
        return Arrays.asList(WHITESPACE.split(rest, Math.max(type.arity(), 1)));
    }

    private static void requireToken(CommandType type, String value) {
        if (!isToken(value)) {
            throw new IllegalArgumentException(type.prefix() + " argument must be a single word: '" + value + "'");
        }
    }

    public static Command auth(String login, String password) {
        return new Command(CommandType.AUTH, login, password);
    }

    public static Command authOk(String username) {
        return new Command(CommandType.AUTH_OK, username);
    }

    public static Command authError(String reason) {
        return new Command(CommandType.AUTH_ERROR, reason);
    }

    public static Command publicMessage(String text) {
        return new Command(CommandType.PUBLIC_MESSAGE, text);
    }

    public static Command privateMessage(String recipient, String text) {
        return new Command(CommandType.PRIVATE_MESSAGE, recipient, text);
    }

    public static Command clientMessage(String sender, String text) {
        return new Command(CommandType.CLIENT_MESSAGE, sender, text);
    }

    public static Command privateDelivery(String sender, String recipient, String text) {
        return new Command(CommandType.PRIVATE_DELIVERY, sender, recipient, text);
    }

    public static Command serverMessage(String text) {
        return new Command(CommandType.SERVER_MESSAGE, text);
    }

    public static Command userList(List<String> usernames) {
        return new Command(CommandType.USER_LIST, usernames);
    }

    public static Command changeUsername(String newUsername) {
        return new Command(CommandType.CHANGE_USERNAME, newUsername);
    }

    public static Command changeUsernameOk(String newUsername) {
        return new Command(CommandType.CHANGE_USERNAME_OK, newUsername);
    }

    public static Command changeUsernameError(String reason) {
        return new Command(CommandType.CHANGE_USERNAME_ERROR, reason);
    }

    public static Command end() {
        return new Command(CommandType.END);
    }
}
