package com.tlenparty.chat.server.domain.model;

import java.util.regex.Pattern;

public final class UsernamePolicy {

    public static final int MAX_LENGTH = 32;

    private static final Pattern ALLOWED = Pattern.compile("\\S{1," + MAX_LENGTH + "}");

    private UsernamePolicy() {
    }

    public static boolean isValid(String username) {
        return username != null && ALLOWED.matcher(username).matches();
    }
}
