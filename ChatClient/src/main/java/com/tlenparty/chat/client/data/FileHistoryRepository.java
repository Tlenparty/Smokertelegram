package com.tlenparty.chat.client.data;

import com.tlenparty.chat.client.domain.HistoryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileHistoryRepository implements HistoryRepository {

    private static final Logger logger = LogManager.getLogger(FileHistoryRepository.class);

    private final Path file;

    public FileHistoryRepository(Path directory, String login) {
        this.file = directory.resolve("chatHistory_" + login + ".txt");
    }

    @Override
    public void append(String entry) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("Не удалось сохранить историю в {}", file, e);
        }
    }

    @Override
    public List<String> lastLines(int count) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return lines.subList(Math.max(0, lines.size() - count), lines.size());
        } catch (IOException e) {
            logger.error("Не удалось прочитать историю из {}", file, e);
            return List.of();
        }
    }
}
