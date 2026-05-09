package com.chatapp.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database configuration for SQLite.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.chatapp.server.repository")
public class SqliteConfig {

    private static final String DB_PATH = "chat-server.db";

    /**
     * Initialize SQLite database with required tables.
     * This is automatically called by Spring Boot JPA/Hibernate,
     * but we ensure the database file exists.
     */
    public static void initializeDatabase() {
        try {
            // Ensure the database directory exists
            File dbFile = new File(DB_PATH);
            if (dbFile.getParentFile() != null) {
                dbFile.getParentFile().mkdirs();
            }

            // Create connection to initialize database
            String url = "jdbc:sqlite:" + DB_PATH;
            try (Connection conn = DriverManager.getConnection(url)) {
                if (conn != null) {
                    System.out.println("SQLite database initialized at: " + dbFile.getAbsolutePath());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    /**
     * Get the database path.
     *
     * @return The database file path
     */
    public static String getDatabasePath() {
        return DB_PATH;
    }
}