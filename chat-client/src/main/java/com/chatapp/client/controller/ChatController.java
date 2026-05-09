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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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

    @FXML
    private Label chatTargetLabel;

    @FXML
    private Button backButton;

    private String username;
    private String password; // Store for reconnection
    private ChatStompClient stompClient;
    private final ObservableList<MessageItem> messages = FXCollections.observableArrayList();
    private final ObservableList<String> users = FXCollections.observableArrayList();

    // Current chat mode: null = broadcast, "all" = broadcast, or username = P2P
    private String currentChatTarget = "all";
    // Store P2P messages separately
    private final Map<String, ObservableList<MessageItem>> p2pMessages = new HashMap<>();
    // Store unread counts for P2P conversations
    private final Map<String, Integer> unreadCounts = new HashMap<>();

    @FXML
    private void initialize() {
        messageList.setItems(messages);
        messageList.setCellFactory(listView -> new MessageCellFactory());

        userListView.setItems(users);
        userListView.setCellFactory(listView -> new UserListCellFactory(unreadCounts));

        // Add click handler for user selection
        userListView.setOnMouseClicked(this::handleUserClick);
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
        updateChatTargetLabel();
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
            // Reset to broadcast mode
            switchToChat("all");
        } else {
            ChatClientApplication.showErrorAndReturn("Login failed: " + response.getMessage());
        }
    }

    /**
     * Handle incoming text messages.
     */
    private void handleTextMessage(TextMessage message) {
        String sender = message.getSender();
        String recipient = message.getRecipient();

        if (recipient != null && !recipient.isEmpty()) {
            // P2P message
            String chatPartner = sender.equals(username) ? recipient : sender;
            boolean isForCurrentChat = chatPartner.equals(currentChatTarget);

            // Get or create the message list for this conversation
            ObservableList<MessageItem> p2pList = p2pMessages.computeIfAbsent(chatPartner,
                k -> FXCollections.observableArrayList());

            String type = sender.equals(username) ? "own" : "text";
            String displayContent = sender.equals(username)
                ? "To " + recipient + ": " + message.getContent()
                : "From " + sender + ": " + message.getContent();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            MessageItem item = new MessageItem(sender.equals(username) ? "You" : sender,
                message.getContent(), timestamp, type);

            p2pList.add(item);

            // Update unread count if not viewing this chat
            if (!isForCurrentChat) {
                int count = unreadCounts.getOrDefault(chatPartner, 0) + 1;
                unreadCounts.put(chatPartner, count);
                // Trigger UI update for user list
                userListView.refresh();
            }

            // If currently viewing P2P chat, show the message
            if (isForCurrentChat) {
                messages.add(item);
                messageList.scrollTo(messages.size() - 1);
            }
        } else {
            // Broadcast message - only show in broadcast mode
            if ("all".equals(currentChatTarget)) {
                String type = sender.equals(username) ? "own" : "text";
                addMessage(sender, message.getContent(), type);
            }
        }
    }

    /**
     * Handle user list updates.
     */
    private void handleUserListMessage(UserListMessage message) {
        users.clear();
        for (String user : message.getUsers()) {
            if (!user.equals(username)) {
                users.add(user);
            }
        }
        // Refresh the user list to show updated unread counts
        userListView.refresh();
    }

    /**
     * Handle system messages.
     */
    private void handleSystemMessage(SystemMessage message) {
        // Only show system messages in broadcast mode
        if ("all".equals(currentChatTarget)) {
            addSystemMessage(message.getContent());
        }
    }

    /**
     * Handle user click to start P2P chat.
     */
    private void handleUserClick(MouseEvent event) {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            switchToChat(selectedUser);
        }
    }

    /**
     * Switch to a specific chat (broadcast or P2P).
     */
    private void switchToChat(String target) {
        if (target.equals(currentChatTarget)) {
            return;
        }

        currentChatTarget = target;
        messages.clear();

        if ("all".equals(target)) {
            // Broadcast chat - no special messages to load
            if (backButton != null) {
                backButton.setVisible(false);
                backButton.setManaged(false);
            }
        } else {
            // P2P chat - load existing messages
            ObservableList<MessageItem> p2pList = p2pMessages.get(target);
            if (p2pList != null) {
                messages.addAll(p2pList);
            }
            // Clear unread count
            unreadCounts.put(target, 0);
            userListView.refresh();

            if (backButton != null) {
                backButton.setVisible(true);
                backButton.setManaged(true);
            }
        }

        updateChatTargetLabel();
    }

    /**
     * Update the chat target label.
     */
    private void updateChatTargetLabel() {
        if ("all".equals(currentChatTarget)) {
            chatTargetLabel.setText("Group Chat");
        } else {
            chatTargetLabel.setText("Chat with: " + currentChatTarget);
        }
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
            if ("all".equals(currentChatTarget)) {
                stompClient.sendTextMessage(text);
            } else {
                stompClient.sendTextMessage(text, currentChatTarget);
            }
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
     * Handle back to group chat button.
     */
    @FXML
    private void handleBackToGroup() {
        switchToChat("all");
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

    /**
     * Custom cell factory for user list with unread indicators.
     */
    private static class UserListCellFactory extends ListCell<String> {
        private final Map<String, Integer> unreadCounts;

        public UserListCellFactory(Map<String, Integer> unreadCounts) {
            this.unreadCounts = unreadCounts;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                Integer unread = unreadCounts.get(item);
                if (unread != null && unread > 0) {
                    setText(item + " (" + unread + ")");
                    getStyleClass().clear();
                    getStyleClass().add("user-unread");
                } else {
                    setText(item);
                    getStyleClass().clear();
                }
            }
        }
    }
}