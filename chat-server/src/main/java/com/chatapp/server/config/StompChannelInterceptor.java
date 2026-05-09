package com.chatapp.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor to set user principal from STOMP CONNECT headers.
 * Clients must send 'username' header in CONNECT frame.
 */
@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StompChannelInterceptor.class);

    // Map to store username by session ID for messaging purposes
    public static final Map<String, String> sessionUsers = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Debug: print session ID
            System.out.println("=== STOMP CONNECT ===");
            System.out.println("Session ID: " + accessor.getSessionId());
            System.out.println("===================");

            // Get username from native header (sent by client as 'username')
            String username = accessor.getFirstNativeHeader("username");

            String password = null;
            if (username == null) {
                username = accessor.getLogin();
            } else {
                password = accessor.getFirstNativeHeader("password");
            }

            System.out.println("StompChannelInterceptor: extracted login=" + username);

            if (username != null && !username.isEmpty()) {
                // Create and set the user principal
                Principal principal = new StompPrincipal(username, password);
                accessor.setUser(principal);

                // Store session ID -> username mapping for convertAndSendToUser to work
                accessor.getSessionAttributes().put("username", username);
                sessionUsers.put(accessor.getSessionId(), username);

                logger.info("StompChannelInterceptor: Set user principal for session {} with username {}",
                    accessor.getSessionId(), username);
            } else {
                logger.warn("StompChannelInterceptor: No login in CONNECT headers");
            }
        } else if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // Clean up on disconnect
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                sessionUsers.remove(sessionId);
                logger.info("StompChannelInterceptor: Removed session {}", sessionId);
            }
        }

        return message;
    }

    /**
     * Simple Principal implementation for STOMP users.
     */
    private static class StompPrincipal implements Principal {
        private final String name;
        private final String password;

        public StompPrincipal(String name, String password) {
            this.name = name;
            this.password = password;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getPassword() {
            return password;
        }
    }
}