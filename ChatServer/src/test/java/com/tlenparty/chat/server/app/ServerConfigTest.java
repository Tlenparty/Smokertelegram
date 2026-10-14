package com.tlenparty.chat.server.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void argumentsTakePrecedenceOverPropertiesAndEnvironment() throws IOException {
        Path argDb = Files.createFile(tempDir.resolve("arg.db"));
        Path propDb = Files.createFile(tempDir.resolve("prop.db"));
        Properties properties = new Properties();
        properties.setProperty(ServerConfig.PORT_PROPERTY, "9001");
        properties.setProperty(ServerConfig.DB_PROPERTY, propDb.toString());

        ServerConfig config = ServerConfig.resolve(new String[]{"9000", argDb.toString()}, properties,
                Map.of(ServerConfig.PORT_ENV, "9002"));

        assertEquals(9000, config.port());
        assertEquals(argDb, config.databasePath());
    }

    @Test
    void fallsBackToEnvironment() throws IOException {
        Path envDb = Files.createFile(tempDir.resolve("env.db"));

        ServerConfig config = ServerConfig.resolve(new String[0], new Properties(),
                Map.of(ServerConfig.PORT_ENV, "9002", ServerConfig.DB_ENV, envDb.toString()));

        assertEquals(9002, config.port());
        assertEquals(envDb, config.databasePath());
    }

    @Test
    void failsFastOnMissingDatabaseOrBadPort() {
        Path missing = tempDir.resolve("missing.db");

        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.resolve(new String[]{"8189", missing.toString()}, new Properties(), Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.resolve(new String[]{"not-a-port"}, new Properties(), Map.of()));
    }
}
