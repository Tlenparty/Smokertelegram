package com.tlenparty.chat.server.app;

import com.tlenparty.chat.server.data.Pbkdf2PasswordHasher;

public final class PasswordHashTool {

    private PasswordHashTool() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: PasswordHashTool <password> [<password>...]");
            System.exit(2);
        }
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        for (String password : args) {
            System.out.println(hasher.hash(password));
        }
    }
}
