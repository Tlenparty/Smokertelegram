package com.tlenparty.chat.common.protocol;

import java.util.Arrays;
import java.util.Optional;

public enum CommandType {
    AUTH("/auth", 2),
    AUTH_OK("/authok", 1),
    AUTH_ERROR("/autherr", 1),
    PUBLIC_MESSAGE("/msg", 1),
    PRIVATE_MESSAGE("/w", 2),
    CLIENT_MESSAGE("/clientMsg", 2),
    PRIVATE_DELIVERY("/privateMsg", 3),
    SERVER_MESSAGE("/serverMsg", 1),
    USER_LIST("/userList", CommandType.VARIADIC),
    CHANGE_USERNAME("/changeUsername", 1),
    CHANGE_USERNAME_OK("/changeUsernameOk", 1),
    CHANGE_USERNAME_ERROR("/changeUsernameErr", 1),
    END("/end", 0);

    static final int VARIADIC = -1;

    private final String prefix;
    private final int arity;

    CommandType(String prefix, int arity) {
        this.prefix = prefix;
        this.arity = arity;
    }

    public String prefix() {
        return prefix;
    }

    int arity() {
        return arity;
    }

    boolean isVariadic() {
        return arity == VARIADIC;
    }

    public static Optional<CommandType> fromPrefix(String prefix) {
        return Arrays.stream(values()).filter(type -> type.prefix.equals(prefix)).findFirst();
    }
}
