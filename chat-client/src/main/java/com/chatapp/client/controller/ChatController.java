package com.chatapp.client.controller;

import com.chatapp.client.ChatClientApplication;
import com.chatapp.client.model.MessageItem;
import com.chatapp.client.network.ChatStompClient;
import com.chatapp.common.model.LoginResponse;
import com.chatapp.common.model.SystemMessage;
import com.chatapp.common.model.TextMessage;
import com.chatapp.common.model.UserListMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the main chat view.
 */
public class ChatController {

    @FXML
    private ListView<MessageItem> messageList;

    @FXML
    private TextField messageField;

    @FXML
    private ListView<String> userListView;

    private String username;
    private String password; // Store for reconnection
    private ChatStompClient stompClient;
    private final ObservableList<MessageItem> messages = FXCollections.observableArrayList();
    private final ObservableList<String> users = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        messageList.setItems(messages);
        messageList.setCellFactory(listView -> new MessageCellFactory());

        userListView.setItems(users);
    }

    /**
     * Set the username and reuse an existing STOMP client.
     */
    public void setConnectedClient(String username, ChatStompClient stompClient) {
        this.username = username;
        this.stompClient = stompClient;

        // Set up message callbacks on the existing connection
        this.stompClient.setCallbacks(
            this::handleLoginResponse,
            this::handleTextMessage,
            this::handleUserListMessage,
            this::handleSystemMessage
        );

        System.out.println("Chat view initialized for: " + username);
        addSystemMessage("Connected as " + username);
    }

    /**
     * Set username only (for backward compatibility).
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Handle login response.
     */
    private void handleLoginResponse(LoginResponse response) {
        if (response.isSuccess()) {
            addSystemMessage("Connected to server as " + response.getUsername());
            // User list will be received via UserListMessage
            users.clear();
        } else {
            ChatClientApplication.showErrorAndReturn("Login failed: " + response.getMessage());
        }
    }

    /**
     * Handle incoming text messages.
     */
    private void handleTextMessage(TextMessage message) {
        String type = message.getSender().equals(username) ? "own" : "text";
        addMessage(message.getSender(), message.getContent(), type);
    }

    /**
     * Handle user list updates.
     */
    private void handleUserListMessage(UserListMessage message) {
        users.clear();
        users.addAll(message.getUsers());
    }

    /**
     * Handle system messages.
     */
    private void handleSystemMessage(SystemMessage message) {
        addSystemMessage(message.getContent());
    }

    /**
     * Add a message to the list.
     */
    private void addMessage(String sender, String content, String type) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        MessageItem item = new MessageItem(sender, content, timestamp, type);
        messages.add(item);
        // Scroll to bottom
        messageList.scrollTo(messages.size() - 1);
    }

    /**
     * Add a system message.
     */
    private void addSystemMessage(String content) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        MessageItem item = new MessageItem("System", content, timestamp, "system");
        messages.add(item);
        messageList.scrollTo(messages.size() - 1);
    }

    /**
     * Send message when Enter key is pressed.
     */
    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode().toString().equals("ENTER")) {
            sendMessage();
        }
    }

    /**
     * Send button clicked.
     */
    @FXML
    private void handleSend() {
        sendMessage();
    }

    /**
     * Send the message.
     */
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty() && stompClient != null && stompClient.isConnected()) {
            stompClient.sendTextMessage(text);
            messageField.clear();
        }
    }

    /**
     * Handle disconnect action.
     */
    @FXML
    private void handleDisconnect() {
        if (stompClient != null) {
            stompClient.disconnect();
        }
        ChatClientApplication.showLoginView();
    }

    /**
     * Clean up on close.
     */
    public void cleanup() {
        if (stompClient != null) {
            stompClient.disconnect();
        }
    }

    /**
     * Custom cell factory for message list.
     */
    private static class MessageCellFactory extends ListCell<MessageItem> {
        @Override
        protected void updateItem(MessageItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String styleClass = item.getType();
                setText(String.format("[%s] %s: %s",
                    item.getTimestamp(),
                    item.getSender(),
                    item.getContent()));
                setStyleClass(styleClass);
            }
        }

        private void setStyleClass(String type) {
            getStyleClass().clear();
            switch (type) {
                case "own":
                    getStyleClass().add("message-own");
                    break;
                case "system":
                    getStyleClass().add("message-system");
                    break;
                default:
                    getStyleClass().add("message-other");
            }
        }
    }
}