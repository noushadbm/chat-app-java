package com.chatapp.server.websocket;

import com.chatapp.common.MessageSerializer;
import com.chatapp.common.model.*;
import com.chatapp.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for chat messages.
 */
@Controller
public class ChatWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();

    public ChatWebSocketHandler(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
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

        // Broadcast to all users
        messagingTemplate.convertAndSend("/topic/messages", message);
        logger.info("Broadcast message from {}: {}", sender, message.getContent());
    }

    private void handleLoginMessage(LoginRequest loginRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        var validatedUser = userService.validateCredentials(username, password);

        if (validatedUser.isPresent()) {
            // Store username in session
            headerAccessor.getSessionAttributes().put("username", username);
            connectedUsers.add(username);

            // Send success response
            LoginResponse response = new LoginResponse(true, "Login successful", username);
            try {
                String responseJson = MessageSerializer.serialize(response);
                // Send to topic for login responses (client can subscribe without auth)
                messagingTemplate.convertAndSend("/topic/login", responseJson);
            } catch (Exception e) {
                logger.error("Error serializing login response", e);
            }

            // Notify others
            SystemMessage systemMsg = new SystemMessage(
                username + " joined the chat",
                SystemMessage.SystemMessageType.USER_JOINED
            );
            messagingTemplate.convertAndSend("/topic/messages", systemMsg);

            // Send updated user list
            broadcastUserList();

            logger.info("User logged in: {}", username);
        } else {
            // Send failure response
            LoginResponse response = new LoginResponse(false, "Invalid credentials");
            try {
                String responseJson = MessageSerializer.serialize(response);
                messagingTemplate.convertAndSend("/topic/errors", responseJson);
            } catch (Exception e) {
                logger.error("Error serializing login response", e);
            }
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
}