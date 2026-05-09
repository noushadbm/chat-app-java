package com.chatapp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Chat Server.
 * Provides both WebSocket chat functionality and web-based user management.
 */
@SpringBootApplication
public class ChatServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServerApplication.class, args);
    }
}