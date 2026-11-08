package com.example.elibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ELibraryServiceApplication {

    public static void main(String[] args) throws IOException {
        ensureSqliteDirectory();
        SpringApplication.run(ELibraryServiceApplication.class, args);
    }

    /**
     * SQLite creates the database file but not its parent directory. Keeping this concern in the
     * bootstrap code means every environment (local shell, Docker, tests) behaves the same way.
     */
    private static void ensureSqliteDirectory() throws IOException {
        String configuredPath = System.getenv().getOrDefault("ELIBRARY_DB_PATH", "./data/library.db");
        Path parent = Path.of(configuredPath).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
