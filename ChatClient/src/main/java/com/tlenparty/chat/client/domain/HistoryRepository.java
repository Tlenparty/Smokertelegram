package com.tlenparty.chat.client.domain;

import java.util.List;

public interface HistoryRepository {

    void append(String entry);

    List<String> lastLines(int count);
}
