package com.chatapp.client.network;

import com.chatapp.common.MessageSerializer;
import com.chatapp.common.model.ChatMessage;
import com.chatapp.common.model.LoginRequest;
import com.chatapp.common.model.LoginResponse;
import com.chatapp.common.model.TextMessage;
import com.chatapp.common.model.UserListMessage;
import com.chatapp.common.model.SystemMessage;
import javafx.application.Platform;

import java.net.URI;
import java.util.function.Consumer;

/**
 * WebSocket client for connecting to the chat server.
 */
public class ChatWebSocketClient {

    private org.java_websocket.client.WebSocketClient webSocketClient;
    private String serverUrl;
    private String username;
    private String password;
    private Consumer<LoginResponse> loginCallback;
    private Consumer<TextMessage> messageCallback;
    private Consumer<UserListMessage> userListCallback;
    private Consumer<SystemMessage> systemCallback;

    public ChatWebSocketClient(String serverUrl) {
        this.serverUrl = serverUrl;
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
            webSocketClient = new org.java_websocket.client.WebSocketClient(new URI(serverUrl)) {
                @Override
                public void onOpen(org.java_websocket.handshake.ServerHandshake handshake) {
                    System.out.println("Connected to server");
                    // Send login request
                    LoginRequest loginRequest = new LoginRequest(username, password);
                    sendMessage(loginRequest);
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected from server: " + reason);
                    Platform.runLater(() -> {
                        systemCallback.accept(new SystemMessage("Disconnected from server: " + reason,
                            SystemMessage.SystemMessageType.GENERAL));
                    });
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                systemCallback.accept(new SystemMessage("Failed to connect: " + e.getMessage(),
                    SystemMessage.SystemMessageType.GENERAL));
            });
        }
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
            System.err.println("Failed to parse message: " + e.getMessage());
        }
    }

    /**
     * Send a message to the server.
     */
    public void sendMessage(ChatMessage message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            try {
                String json = MessageSerializer.serialize(message);
                webSocketClient.send(json);
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }

    /**
     * Send a text message.
     */
    public void sendTextMessage(String text) {
        TextMessage textMessage = new TextMessage(username, text);
        sendMessage(textMessage);
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }
}