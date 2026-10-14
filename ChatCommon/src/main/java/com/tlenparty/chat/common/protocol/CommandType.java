package com.tlenparty.chat.common.protocol;

import java.util.Arrays;
import java.util.Optional;

public enum CommandType {
    AUTH("/auth", 2, true),
    AUTH_OK("/authok", 1, false),
    AUTH_ERROR("/autherr", 1, true),
    PUBLIC_MESSAGE("/msg", 1, true),
    PRIVATE_MESSAGE("/w", 2, true),
    CLIENT_MESSAGE("/clientMsg", 2, true),
    PRIVATE_DELIVERY("/privateMsg", 3, true),
    SERVER_MESSAGE("/serverMsg", 1, true),
    USER_LIST("/userList", CommandType.VARIADIC, false),
    CHANGE_USERNAME("/changeUsername", 1, false),
    CHANGE_USERNAME_OK("/changeUsernameOk", 1, false),
    CHANGE_USERNAME_ERROR("/changeUsernameErr", 1, true),
    END("/end", 0, false);

    static final int VARIADIC = -1;

    private final String prefix;
    private final int arity;
    private final boolean freeTextTail;

    CommandType(String prefix, int arity, boolean freeTextTail) {
        this.prefix = prefix;
        this.arity = arity;
        this.freeTextTail = freeTextTail;
    }

    public String prefix() {
        return prefix;
    }

    int arity() {
        return arity;
    }

    boolean hasFreeTextTail() {
        return freeTextTail;
    }

    boolean isVariadic() {
        return arity == VARIADIC;
    }

    public static Optional<CommandType> fromPrefix(String prefix) {
        return Arrays.stream(values()).filter(type -> type.prefix.equals(prefix)).findFirst();
    }
}
