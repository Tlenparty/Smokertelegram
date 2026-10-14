package com.tlenparty.chat.client.presentation;

@FunctionalInterface
public interface ErrorReporter {

    void report(String title, String header, String message);
}
