package com.chatapp.server.websocket;

import com.chatapp.common.MessageSerializer;
import com.chatapp.common.model.*;
import com.chatapp.server.service.FileService;
import com.chatapp.server.service.MessageService;
import com.chatapp.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.chatapp.server.config.StompChannelInterceptor;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.chatapp.common.model.FileMessage;

/**
 * WebSocket handler for chat messages.
 */
@Controller
public class ChatWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final UserService userService;
    private final MessageService messageService;
    private final FileService fileService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    // Map username to their session ID for direct messaging
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(UserService userService, MessageService messageService,
                                  FileService fileService,
                                  SimpMessagingTemplate messagingTemplate,
                                  SimpUserRegistry userRegistry) {
        this.userService = userService;
        this.messageService = messageService;
        this.fileService = fileService;
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
            } else if (message instanceof FileMessage fileMessage) {
                handleFileMessage(fileMessage, headerAccessor);
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

    private void handleFileMessage(FileMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sender = message.getSender();
        String recipient = message.getRecipient();

        // We do not save the file here — it was already uploaded via HTTP and stored by FileService.
        // We only route the metadata message.

        if (recipient != null && !recipient.isEmpty()) {
            // P2P file
            String recipientTopic = "/topic/user/" + recipient + "/messages";
            String senderTopic = "/topic/user/" + sender + "/messages";

            messagingTemplate.convertAndSend(recipientTopic, message);
            messagingTemplate.convertAndSend(senderTopic, message);
            logger.info("P2P file shared from {} to {}: {}", sender, recipient, message.getOriginalFilename());
        } else {
            // Group file
            messagingTemplate.convertAndSend("/topic/messages", message);
            logger.info("Group file shared by {}: {}", sender, message.getOriginalFilename());
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

            // Get user display name
            String displayName = username;
            var userOpt = userService.getUserByUsername(username);
            if (userOpt.isPresent()) {
                displayName = userOpt.get().getDisplayName();
            }

            // Send success response with display name
            LoginResponse response = new LoginResponse(true, "Login successful", username, displayName);
            try {
                String responseJson = MessageSerializer.serialize(response);
                // Send to topic for login responses (client can subscribe without auth)
                messagingTemplate.convertAndSend("/topic/login", responseJson);
            } catch (Exception e) {
                logger.error("Error serializing login response", e);
            }

            // Always persist the "joined" event (for history)
            long timestamp = System.currentTimeMillis();
            String joinContent = username + " joined the chat";
            messageService.saveMessage(username, joinContent, null, timestamp, "system", "USER_JOINED");

            // Notify currently connected users (only if this is not the very first user)
            if (!isFirstUser) {
                SystemMessage systemMsg = new SystemMessage(
                    joinContent,
                    SystemMessage.SystemMessageType.USER_JOINED
                );
                systemMsg.setTimestamp(timestamp);
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
                    SystemMessage.SystemMessageType sysType = SystemMessage.SystemMessageType.GENERAL;
                    String stored = msg.getSystemMessageType();
                    if ("USER_JOINED".equals(stored)) {
                        sysType = SystemMessage.SystemMessageType.USER_JOINED;
                    } else if ("USER_LEFT".equals(stored)) {
                        sysType = SystemMessage.SystemMessageType.USER_LEFT;
                    } else if ("SERVER_SHUTDOWN".equals(stored)) {
                        sysType = SystemMessage.SystemMessageType.SERVER_SHUTDOWN;
                    }
                    SystemMessage systemMsg = new SystemMessage(msg.getContent(), sysType);
                    systemMsg.setTimestamp(msg.getTimestamp());
                    messagingTemplate.convertAndSend("/topic/user/" + username + "/history", systemMsg);
                } else {
                    TextMessage textMsg = new TextMessage(msg.getSender(), msg.getContent(), msg.getRecipient());
                    textMsg.setTimestamp(msg.getTimestamp());
                    messagingTemplate.convertAndSend("/topic/user/" + username + "/history", textMsg);
                }
            }
            logger.debug("Sent {} messages to chat history for user {}", history.size(), username);

            // Also send recent file shares
            long since = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            List<com.chatapp.server.model.FileMetadata> recentFiles = fileService.getRecentFilesForUser(username, since);

            for (com.chatapp.server.model.FileMetadata f : recentFiles) {
                FileMessage fm = new FileMessage(
                        f.getSender(),
                        f.getRecipient(),
                        f.getFileId(),
                        f.getOriginalFilename(),
                        f.getSize(),
                        f.getContentType()
                );
                fm.setTimestamp(f.getTimestamp());
                messagingTemplate.convertAndSend("/topic/user/" + username + "/history", fm);
            }

        } catch (Exception e) {
            logger.error("Error sending chat history to user {}: {}", username, e.getMessage());
        }
    }

    private void broadcastUserList() {
        // Get ALL active users from database (online + offline)
        List<com.chatapp.server.model.User> allActiveUsers = userService.getAllActiveUsers();
        List<String> allUsernames = allActiveUsers.stream()
                .map(com.chatapp.server.model.User::getUsername)
                .toList();

        Map<String, String> displayNames = new HashMap<>();
        for (com.chatapp.server.model.User user : allActiveUsers) {
            String dn = user.getDisplayName();
            displayNames.put(user.getUsername(), (dn != null && !dn.isEmpty()) ? dn : user.getUsername());
        }

        // Currently connected = online
        List<String> onlineList = connectedUsers.stream().toList();

        UserListMessage userListMessage = new UserListMessage(allUsernames, displayNames, onlineList);
        messagingTemplate.convertAndSend("/topic/users", userListMessage);
    }

    /**
     * Get connected users count.
     */
    public int getConnectedUsersCount() {
        return connectedUsers.size();
    }

    /**
     * Handle WebSocket disconnect events (network drop, client close, etc.)
     * Removes user from connected set and refreshes the user list for everyone.
     */
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String username = null;

        // 1. Try principal (most reliable)
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            username = principal.getName();
        }

        // 2. Try session attributes (set during CONNECT and login)
        if (username == null) {
            username = (String) headerAccessor.getSessionAttributes().get("username");
        }

        // 3. Fallback to the static map (for abrupt disconnects)
        if (username == null && sessionId != null) {
            username = StompChannelInterceptor.sessionUsers.remove(sessionId);
        }

        if (username != null) {
            connectedUsers.remove(username);
            userSessions.remove(username);
            if (sessionId != null) {
                StompChannelInterceptor.sessionUsers.remove(sessionId);
            }
            logger.info("User disconnected: {}", username);

            // Persist and broadcast "left" system message so it appears in history for others
            long ts = System.currentTimeMillis();
            String leftContent = username + " left the chat";
            messageService.saveMessage(username, leftContent, null, ts, "system", "USER_LEFT");

            SystemMessage leftMsg = new SystemMessage(leftContent, SystemMessage.SystemMessageType.USER_LEFT);
            leftMsg.setTimestamp(ts);
            messagingTemplate.convertAndSend("/topic/messages", leftMsg);

            broadcastUserList();
        }
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