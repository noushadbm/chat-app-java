package com.chatapp.client.network;

import com.chatapp.common.MessageSerializer;
import com.chatapp.common.model.*;
import javafx.application.Platform;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * STOMP WebSocket client for connecting to the chat server.
 */
public class ChatStompClient implements StompSessionHandler {

    private StompSession stompSession;
    private final String serverUrl;
    private String username;
    private String password;
    private Consumer<LoginResponse> loginCallback;
    private Consumer<TextMessage> messageCallback;
    private Consumer<UserListMessage> userListCallback;
    private Consumer<SystemMessage> systemCallback;

    public ChatStompClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Set callbacks after connection is established.
     */
    public void setCallbacks(Consumer<LoginResponse> loginCallback,
                             Consumer<TextMessage> messageCallback,
                             Consumer<UserListMessage> userListCallback,
                             Consumer<SystemMessage> systemCallback) {
        this.loginCallback = loginCallback;
        this.messageCallback = messageCallback;
        this.userListCallback = userListCallback;
        this.systemCallback = systemCallback;
    }

    /**
     * Connect to the server with credentials.
     */
    public void connect(String username, String password,
                        Consumer<LoginResponse> loginCallback,
                        Consumer<TextMessage> messageCallback,
                        Consumer<UserListMessage> userListCallback,
                        Consumer<SystemMessage> systemCallback) {
        this.username = username;
        this.password = password;
        this.loginCallback = loginCallback;
        this.messageCallback = messageCallback;
        this.userListCallback = userListCallback;
        this.systemCallback = systemCallback;

        try {
            WebSocketClient webSocketClient = new StandardWebSocketClient();
            WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);

            // Use STOMP standard headers (login/passcode) for user identification
            StompHeaders connectHeaders = new StompHeaders();
            // Use native headers which are sent as custom headers in CONNECT frame
            connectHeaders.add("username", username);
            connectHeaders.add("password", password);

            System.out.println("Connecting with headers: username=" + username);
            stompClient.connect(serverUrl, this, connectHeaders);
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                systemCallback.accept(new SystemMessage("Failed to connect: " + e.getMessage(),
                    SystemMessage.SystemMessageType.GENERAL));
            });
        }
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders headers) {
        System.out.println("Connected to STOMP server");
        stompSession = session;

        // Subscribe to login response topic
        session.subscribe("/topic/login", this);

        // Subscribe to broadcast messages
        session.subscribe("/topic/messages", this);

        // Subscribe to user list updates
        session.subscribe("/topic/users", this);

        // Subscribe to personal P2P messages (user-specific topic)
        // Server sends P2P messages to /topic/user/{username}/messages
        session.subscribe("/topic/user/" + username + "/messages", this);

        // Subscribe to chat history (messages from last 24 hours)
        session.subscribe("/topic/user/" + username + "/history", this);

        // Send login request as bytes
        try {
            LoginRequest loginRequest = new LoginRequest(username, password);
            String json = MessageSerializer.serialize(loginRequest);
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            session.send("/app/chat", payload);
            System.out.println("Login request sent");
        } catch (Exception e) {
            System.err.println("Error sending login request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        byte[] bytes = null;

        if (payload instanceof byte[]) {
            bytes = (byte[]) payload;
        } else if (payload instanceof String) {
            try {
                bytes = ((String) payload).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println("Error converting string to bytes: " + e.getMessage());
                return;
            }
        }

        if (bytes != null) {
            try {
                String json = new String(bytes, StandardCharsets.UTF_8);
                handleMessage(json);
            } catch (Exception e) {
                System.err.println("Error converting bytes to string: " + e.getMessage());
            }
        }
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable ex) {
        System.err.println("STOMP error: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void handleTransportError(StompSession session, Throwable ex) {
        System.err.println("Transport error: " + ex.getMessage());
        Platform.runLater(() -> {
            systemCallback.accept(new SystemMessage("Connection lost: " + ex.getMessage(),
                SystemMessage.SystemMessageType.GENERAL));
        });
    }

    /**
     * Handle incoming message.
     */
    private void handleMessage(String json) {
        try {
            ChatMessage message = MessageSerializer.deserialize(json);
            Platform.runLater(() -> {
                if (message instanceof LoginResponse) {
                    loginCallback.accept((LoginResponse) message);
                } else if (message instanceof TextMessage) {
                    messageCallback.accept((TextMessage) message);
                } else if (message instanceof UserListMessage) {
                    userListCallback.accept((UserListMessage) message);
                } else if (message instanceof SystemMessage) {
                    systemCallback.accept((SystemMessage) message);
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to parse message: " + e.getMessage() + " | JSON: " + json);
        }
    }

    /**
     * Send a text message.
     */
    public void sendTextMessage(String text) {
        sendTextMessage(text, null);
    }

    /**
     * Send a text message to a specific recipient (P2P) or broadcast.
     */
    public void sendTextMessage(String text, String recipient) {
        if (stompSession != null && stompSession.isConnected()) {
            try {
                TextMessage textMessage;
                if (recipient != null && !recipient.isEmpty()) {
                    textMessage = new TextMessage(username, text, recipient);
                } else {
                    textMessage = new TextMessage(username, text);
                }
                String json = MessageSerializer.serialize(textMessage);
                byte[] payload = json.getBytes(StandardCharsets.UTF_8);
                stompSession.send("/app/chat", payload);
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if (stompSession != null) {
            stompSession.disconnect();
        }
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}