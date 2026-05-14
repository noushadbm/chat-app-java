package com.chatapp.server.websocket;

import com.chatapp.common.MessageSerializer;
import com.chatapp.common.model.*;
import com.chatapp.server.service.MessageService;
import com.chatapp.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for chat messages.
 */
@Controller
public class ChatWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final UserService userService;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    // Map username to their session ID for direct messaging
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(UserService userService, MessageService messageService,
                                 SimpMessagingTemplate messagingTemplate,
                                 SimpUserRegistry userRegistry) {
        this.userService = userService;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
    }

    /**
     * Handle incoming chat messages.
     */
    @MessageMapping("/chat")
    public void handleChatMessage(@Payload String messageJson, SimpMessageHeaderAccessor headerAccessor) {
        try {
            ChatMessage message = MessageSerializer.deserialize(messageJson);

            if (message instanceof TextMessage textMessage) {
                handleTextMessage(textMessage, headerAccessor);
            } else if (message instanceof LoginRequest loginRequest) {
                handleLoginMessage(loginRequest, headerAccessor);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage());
        }
    }

    private void handleTextMessage(TextMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sender = message.getSender();
        String recipient = message.getRecipient();
        long timestamp = message.getTimestamp();

        // Save message to database
        messageService.saveTextMessage(sender, message.getContent(), recipient);

        if (recipient != null && !recipient.isEmpty()) {
            // P2P message - send only to specific recipient
            // Store message ID for identification
            String messageId = sender + "_" + System.currentTimeMillis();
            message.setMessageId(messageId);

            // Use user registry to find the recipient's session and send directly
            // The recipient subscribes to /user/{username}/queue/messages
            // But since convertAndSendToUser requires a Principal, we use convertAndSend to a user-specific topic
            String recipientTopic = "/topic/user/" + recipient + "/messages";
            String senderTopic = "/topic/user/" + sender + "/messages";

            messagingTemplate.convertAndSend(recipientTopic, message);
            messagingTemplate.convertAndSend(senderTopic, message);
            logger.info("P2P message from {} to {} via topics: recipient={}, sender={}",
                sender, recipient, recipientTopic, senderTopic);
        } else {
            // Broadcast to all users
            messagingTemplate.convertAndSend("/topic/messages", message);
            logger.info("Broadcast message from {}: {}", sender, message.getContent());
        }
    }

    private void handleLoginMessage(LoginRequest loginRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        var validatedUser = userService.validateCredentials(username, password);

        if (validatedUser.isPresent()) {
            // Store username in session attributes
            headerAccessor.getSessionAttributes().put("username", username);
            boolean isFirstUser = connectedUsers.isEmpty();
            connectedUsers.add(username);

            // Store session ID for direct messaging
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                userSessions.put(username, sessionId);
            }

            // Set the user principal so convertAndSendToUser works
            // The SimpMessageHeaderAccessor handles setting the user principal from session attributes
            // We need to create a Principal and set it
            Principal userPrincipal = new ChatUserPrincipal(username);
            headerAccessor.setUser(userPrincipal);

            // Send success response
            LoginResponse response = new LoginResponse(true, "Login successful", username);
            try {
                String responseJson = MessageSerializer.serialize(response);
                // Send to topic for login responses (client can subscribe without auth)
                messagingTemplate.convertAndSend("/topic/login", responseJson);
            } catch (Exception e) {
                logger.error("Error serializing login response", e);
            }

            // Notify others (only if not the first user)
            if (!isFirstUser) {
                long timestamp = System.currentTimeMillis();
                SystemMessage systemMsg = new SystemMessage(
                    username + " joined the chat",
                    SystemMessage.SystemMessageType.USER_JOINED
                );
                systemMsg.setTimestamp(timestamp);
                // Save system message to database
                messageService.saveMessage(username, username + " joined the chat", null, timestamp, "system");
                messagingTemplate.convertAndSend("/topic/messages", systemMsg);
            }

            // Send updated user list
            broadcastUserList();

            // Send chat history to the newly logged in user
            sendChatHistory(username);

            logger.info("User logged in: {}", username);
        } else {
            // Send failure response
            LoginResponse response = new LoginResponse(false, "Invalid credentials");
            try {
                String responseJson = MessageSerializer.serialize(response);
                // Send to same topic as success response so client can receive it
                messagingTemplate.convertAndSend("/topic/login", responseJson);
            } catch (Exception e) {
                logger.error("Error serializing login response", e);
            }
        }
    }

    /**
     * Send chat history to a user after login.
     */
    private void sendChatHistory(String username) {
        try {
            List<com.chatapp.server.model.Message> history = messageService.getChatHistory(username);
            for (com.chatapp.server.model.Message msg : history) {
                // Convert stored message back to TextMessage or SystemMessage
                if ("system".equals(msg.getMessageType())) {
                    SystemMessage systemMsg = new SystemMessage(
                        msg.getContent(),
                        SystemMessage.SystemMessageType.USER_JOINED
                    );
                    systemMsg.setTimestamp(msg.getTimestamp());
                    messagingTemplate.convertAndSend("/topic/user/" + username + "/history", systemMsg);
                } else {
                    TextMessage textMsg = new TextMessage(msg.getSender(), msg.getContent(), msg.getRecipient());
                    textMsg.setTimestamp(msg.getTimestamp());
                    messagingTemplate.convertAndSend("/topic/user/" + username + "/history", textMsg);
                }
            }
            logger.debug("Sent {} messages to chat history for user {}", history.size(), username);
        } catch (Exception e) {
            logger.error("Error sending chat history to user {}: {}", username, e.getMessage());
        }
    }

    private void broadcastUserList() {
        UserListMessage userListMessage = new UserListMessage(connectedUsers.stream().toList());
        messagingTemplate.convertAndSend("/topic/users", userListMessage);
    }

    /**
     * Get connected users count.
     */
    public int getConnectedUsersCount() {
        return connectedUsers.size();
    }

    /**
     * Simple Principal implementation for WebSocket users.
     */
    private static class ChatUserPrincipal implements Principal {
        private final String name;

        public ChatUserPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}