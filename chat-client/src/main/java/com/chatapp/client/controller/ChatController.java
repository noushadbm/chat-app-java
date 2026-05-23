package com.chatapp.client.controller;

import com.chatapp.client.ChatClientApplication;
import com.chatapp.client.model.MessageItem;
import com.chatapp.client.network.ChatStompClient;
import com.chatapp.common.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the main chat view.
 */
public class ChatController {

    @FXML
    private TextField messageField;

    @FXML
    private ListView<String> userListView;

    @FXML
    private Label chatTargetLabel;

    @FXML
    private Button backButton;

    @FXML
    private VBox messageContainer;

    @FXML
    private ScrollPane messageScrollPane;

    private String username;
    private String displayName; // User's display name
    private String password; // Store for reconnection
    private ChatStompClient stompClient;
    private final ObservableList<String> users = FXCollections.observableArrayList();
    // Store display names for other users
    private final Map<String, String> userDisplayNames = new HashMap<>();
    // Store group chat messages persistently
    private final ObservableList<MessageItem> groupMessages = FXCollections.observableArrayList();

    // Current chat mode: "all" = broadcast, or username = P2P
    private String currentChatTarget = "all";
    // Store P2P messages separately
    private final Map<String, ObservableList<MessageItem>> p2pMessages = new HashMap<>();
    // Store unread counts for P2P conversations
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    // Track which users are currently online (from server UserListMessage)
    private final java.util.Set<String> onlineUsernames = new java.util.HashSet<>();

    // Timestamp when we successfully logged in (used to ignore historical P2P messages for unread counts)
    private long loginTimestamp = 0;

    /**
     * Get display name for a username.
     */
    private String getDisplayName(String username) {
        if (username.equals(this.username)) {
            return this.displayName != null ? this.displayName : username;
        }
        return userDisplayNames.getOrDefault(username, username);
    }

    /**
     * Formats a message timestamp for display.
     * Always shows date + time (e.g. "May 23, 20:42").
     */
    private String formatDisplayTime(long timestampMillis) {
        LocalDateTime messageTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestampMillis),
            ZoneId.systemDefault()
        );
        LocalDate messageDate = messageTime.toLocalDate();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (messageDate.getYear() == today.getYear()) {
            return messageTime.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"));
        } else {
            return messageTime.format(DateTimeFormatter.ofPattern("MMM d yyyy, HH:mm"));
        }
    }

    @FXML
    private void initialize() {
        userListView.setItems(users);
        userListView.setCellFactory(listView -> new UserListCellFactory(unreadCounts, userDisplayNames::get, onlineUsernames::contains));

        // Add click handler for user selection
        userListView.setOnMouseClicked(this::handleUserClick);

        // Make ScrollPane stretch its content to full width so HBox alignment works
        messageScrollPane.setFitToWidth(true);

        // Auto-scroll when message container's children change
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollToBottom();
        });
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
                this::handleSystemMessage,
                this::handleFileMessage
        );

        System.out.println("Chat view initialized for: " + username);
        this.loginTimestamp = System.currentTimeMillis();
        unreadCounts.clear();
        addSystemMessageItem("Connected as " + username);
        updateChatTargetLabel();

        // Scroll to bottom when chat view opens
        scrollToBottom();
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
            // Store display name
            this.displayName = response.getDisplayName();
            this.loginTimestamp = System.currentTimeMillis();   // mark login time so we can ignore old history messages for unread counts
            unreadCounts.clear();                               // start fresh for this session
            addSystemMessageItem("Connected to server as " + this.displayName);
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
            String timeStr = formatDisplayTime(message.getTimestamp());
            String senderDisplayName = sender.equals(username) ? "You" : getDisplayName(sender);
            MessageItem item = new MessageItem(senderDisplayName, message.getContent(), timeStr, type);

            p2pList.add(item);

            // Update unread count only for *new* messages received after login (ignore historical P2P messages)
            if (!isForCurrentChat && this.loginTimestamp > 0 && message.getTimestamp() >= this.loginTimestamp) {
                int count = unreadCounts.getOrDefault(chatPartner, 0) + 1;
                unreadCounts.put(chatPartner, count);
                // Trigger UI update for user list
                userListView.refresh();
            }

            // If currently viewing P2P chat, show the message
            if (isForCurrentChat) {
                addMessageToUI(item);
                scrollToBottom();
            }
        } else {
            // Broadcast message
            String type = sender.equals(username) ? "own" : "text";
            String timeStr = formatDisplayTime(message.getTimestamp());
            String senderDisplayName = getDisplayName(sender);
            MessageItem item = new MessageItem(senderDisplayName, message.getContent(), timeStr, type);
            groupMessages.add(item);

            // Only show in UI if currently viewing group chat
            if ("all".equals(currentChatTarget)) {
                addMessageToUI(item);
                scrollToBottom();
            }
        }
    }

    /**
     * Handle user list updates.
     */
    private void handleUserListMessage(UserListMessage message) {
        users.clear();
        // Replace display names and online status with fresh data from server
        userDisplayNames.clear();
        onlineUsernames.clear();

        if (message.getDisplayNames() != null) {
            userDisplayNames.putAll(message.getDisplayNames());
        }
        if (message.getOnlineUsers() != null) {
            onlineUsernames.addAll(message.getOnlineUsers());
        }

        for (String user : message.getUsers()) {
            if (!user.equals(username)) {
                users.add(user);
            }
        }
        // Refresh the user list to show updated online/offline colors and unread counts
        userListView.refresh();
    }

    /**
     * Handle system messages.
     */
    private void handleSystemMessage(SystemMessage message) {
        addSystemMessageItem(message.getContent());
        scrollToBottom();
    }

    private void handleFileMessage(FileMessage message) {
        String sender = message.getSender();
        String recipient = message.getRecipient();

        boolean isP2P = recipient != null && !recipient.isEmpty();
        String chatPartner = isP2P ? (sender.equals(username) ? recipient : sender) : null;

        boolean isOwn = sender.equals(username);
        String displaySender = isOwn ? "You" : getDisplayName(sender);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // Create MessageItem for file (using extended constructor)
        MessageItem fileItem = new MessageItem(
                displaySender,
                timestamp,
                isOwn ? "file-own" : "file",
                message.getFileId(),
                message.getOriginalFilename(),
                message.getSize(),
                message.getContentType()
        );

        if (isP2P) {
            // P2P File
            boolean isForCurrentChat = chatPartner.equals(currentChatTarget);

            ObservableList<MessageItem> p2pList = p2pMessages.computeIfAbsent(chatPartner,
                    k -> FXCollections.observableArrayList());
            p2pList.add(fileItem);

            if (!isForCurrentChat) {
                int count = unreadCounts.getOrDefault(chatPartner, 0) + 1;
                unreadCounts.put(chatPartner, count);
                userListView.refresh();
            }

            if (isForCurrentChat) {
                addMessageToUI(fileItem);
                scrollToBottom();
            }
        } else {
            // Group File
            groupMessages.add(fileItem);

            if ("all".equals(currentChatTarget)) {
                addMessageToUI(fileItem);
                scrollToBottom();
            }
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
        messageContainer.getChildren().clear();

        if ("all".equals(target)) {
            // Load group chat messages
            for (MessageItem item : groupMessages) {
                addMessageToUI(item);
            }
            if (backButton != null) {
                backButton.setVisible(false);
                backButton.setManaged(false);
            }
        } else {
            // P2P chat - load existing messages
            ObservableList<MessageItem> p2pList = p2pMessages.get(target);
            if (p2pList != null) {
                for (MessageItem item : p2pList) {
                    addMessageToUI(item);
                }
            }
            // Clear unread count
            unreadCounts.put(target, 0);
            userListView.refresh();

            if (backButton != null) {
                backButton.setVisible(true);
                backButton.setManaged(true);
            }
        }

        // Scroll to bottom of the messages
        scrollToBottom();

        updateChatTargetLabel();
    }

    /**
     * Add a message bubble to the UI.
     */
    private void addMessageToUI(MessageItem item) {
        VBox messageBubble = new VBox();
        messageBubble.setSpacing(2);
        messageBubble.setPadding(new Insets(8, 12, 8, 12));

        if ("system".equals(item.getType())) {
            // System messages - centered, no bubble styling
            Label systemLabel = new Label("[" + item.getTimestamp() + "] " + item.getContent());
            systemLabel.getStyleClass().add("message-system");
            messageBubble.getChildren().add(systemLabel);
            messageContainer.getChildren().add(messageBubble);
        } else if (item.getType() != null && item.getType().startsWith("file")) {
            // File message card
            addFileCard(item, messageContainer);
        } else {
            // Sender label
            Label senderLabel = new Label(item.getSender());
            senderLabel.getStyleClass().add("message-sender");

            // Content label
            Label contentLabel = new Label(item.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(300);
            contentLabel.getStyleClass().add("message-content");

            // Time label
            Label timeLabel = new Label(item.getTimestamp());
            timeLabel.getStyleClass().add("message-time");

            messageBubble.getChildren().addAll(senderLabel, contentLabel, timeLabel);

            if ("own".equals(item.getType())) {
                // Right-align own messages - blue bubble
                messageBubble.getStyleClass().add("message-bubble-own");
                // Use setRight() so the bubble actually sits on the right side
                BorderPane container = new BorderPane();
                container.setRight(messageBubble);
                container.setPadding(new Insets(5, 10, 5, 10));
                messageContainer.getChildren().add(container);
            } else {
                // Left-align other messages - white bubble
                messageBubble.getStyleClass().add("message-bubble-other");
                // Use setLeft() so the bubble sits on the left side
                BorderPane container = new BorderPane();
                container.setLeft(messageBubble);
                container.setPadding(new Insets(5, 10, 5, 10));
                messageContainer.getChildren().add(container);
            }
        }
    }

    /**
     * Render a nice file card for shared files.
     */
    private void addFileCard(MessageItem item, VBox container) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMaxWidth(280);
        card.getStyleClass().add("file-card");

        Label nameLabel = new Label(item.getOriginalFilename());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("file-name");

        String sizeStr = formatFileSize(item.getFileSize());
        Label sizeLabel = new Label(sizeStr);
        sizeLabel.getStyleClass().add("file-size");

        Button downloadBtn = new Button("⬇ Download");
        downloadBtn.getStyleClass().add("download-button");
        downloadBtn.setOnAction(e -> downloadFile(item.getFileId(), item.getOriginalFilename()));

        card.getChildren().addAll(nameLabel, sizeLabel, downloadBtn);

        // Sender info
        if (item.getSender() != null && !item.getSender().isEmpty()) {
            Label senderLabel = new Label(item.getSender());
            senderLabel.getStyleClass().add("file-sender");
            card.getChildren().add(0, senderLabel);
        }

        BorderPane wrapper = new BorderPane();
        wrapper.setPadding(new Insets(4, 8, 4, 8));

        boolean isOwn = "file-own".equals(item.getType());
        if (isOwn) {
            wrapper.setRight(card);
            card.getStyleClass().add("file-card-own");
        } else {
            wrapper.setLeft(card);
            card.getStyleClass().add("file-card-other");
        }

        container.getChildren().add(wrapper);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private void downloadFile(String fileId, String filename) {
        if (fileId == null) return;

        String downloadUrl = "http://localhost:8080/api/files/download/" + fileId;

        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(downloadUrl))
                        .build();

                java.net.http.HttpResponse<byte[]> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    Platform.runLater(() -> {
                        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
                        chooser.setInitialFileName(filename);
                        java.io.File saveFile = chooser.showSaveDialog(
                                com.chatapp.client.ChatClientApplication.getPrimaryStage()
                        );
                        if (saveFile != null) {
                            try {
                                java.nio.file.Files.write(saveFile.toPath(), response.body());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Add a system message to the UI (uses current time).
     */
    private void addSystemMessageItem(String content) {
        addSystemMessageItem(content, System.currentTimeMillis());
    }

    /**
     * Add a system message to the UI with a specific timestamp.
     */
    private void addSystemMessageItem(String content, long timestampMillis) {
        String timeStr = formatDisplayTime(timestampMillis);
        MessageItem item = new MessageItem("System", content, timeStr, "system");

        // System messages are part of the group chat history (like broadcast text messages)
        groupMessages.add(item);

        // Only render immediately if we are currently viewing the group chat
        if ("all".equals(currentChatTarget)) {
            addMessageToUI(item);
        }
    }

    /**
     * Scroll to the bottom of the message area.
     */
    private void scrollToBottom() {
        // Use Timeline to delay scroll slightly to ensure layout is complete
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            messageScrollPane.setVvalue(1.0);
        }));
        timeline.play();
    }

    /**
     * Update the chat target label.
     */
    private void updateChatTargetLabel() {
        if ("all".equals(currentChatTarget)) {
            chatTargetLabel.setText("Group Chat");
        } else {
            String targetDisplayName = getDisplayName(currentChatTarget);
            chatTargetLabel.setText("Chat with: " + targetDisplayName);
        }
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

    @FXML
    private void handleAttachFile() {
        if (stompClient == null || !stompClient.isConnected()) {
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select file to share");
        java.io.File selectedFile = fileChooser.showOpenDialog(
                com.chatapp.client.ChatClientApplication.getPrimaryStage()
        );

        if (selectedFile != null) {
            uploadAndShareFile(selectedFile);
        }
    }

    private void uploadAndShareFile(java.io.File file) {
        // TODO: Make HTTP base URL configurable (currently assumes same machine as WS)
        String uploadUrl = "http://localhost:8080/api/files/upload";

        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(uploadUrl))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("username", username)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(
                                buildMultipartBody(file, boundary)
                        ))
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    String fileId = extractJsonValue(body, "fileId");
                    String filename = extractJsonValue(body, "filename");
                    long size = Long.parseLong(extractJsonValue(body, "size").replaceAll("[^0-9]", ""));

                    // Send FileMessage over WebSocket (real-time announcement)
                    String recipient = "all".equals(currentChatTarget) ? null : currentChatTarget;

                    com.chatapp.common.model.FileMessage fm = new com.chatapp.common.model.FileMessage(
                            username, recipient, fileId, filename, size, "application/octet-stream"
                    );

                    stompClient.sendChatMessage(fm);

                } else {
                    System.err.println("File upload failed: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Very basic JSON value extractor (for demo)
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(',', start);
            if (end == -1) end = json.indexOf('}', start);
            return json.substring(start, end).trim().replace("\"", "");
        }
        start += search.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private byte[] buildMultipartBody(java.io.File file, String boundary) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintWriter writer = new java.io.PrintWriter(baos, true, java.nio.charset.StandardCharsets.UTF_8);

        String CRLF = "\r\n";

        // File part
        writer.append("--").append(boundary).append(CRLF);
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
              .append(file.getName()).append("\"").append(CRLF);
        writer.append("Content-Type: application/octet-stream").append(CRLF);
        writer.append(CRLF).flush();

        java.nio.file.Files.copy(file.toPath(), baos);
        baos.flush();

        writer.append(CRLF).flush();
        writer.append("--").append(boundary).append("--").append(CRLF);
        writer.flush();

        return baos.toByteArray();
    }

    // Helper to get server base (placeholder)
    private String serverUrlFromStomp() {
        // TODO: Store the actual base HTTP URL when connecting
        return "http://localhost:8080";
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
     * Custom cell factory for user list with unread indicators, display names, and online/offline colors.
     */
    private static class UserListCellFactory extends ListCell<String> {
        private final Map<String, Integer> unreadCounts;
        private final java.util.function.Function<String, String> displayNameProvider;
        private final java.util.function.Function<String, Boolean> isOnlineProvider;

        public UserListCellFactory(Map<String, Integer> unreadCounts,
                                   java.util.function.Function<String, String> displayNameProvider,
                                   java.util.function.Function<String, Boolean> isOnlineProvider) {
            this.unreadCounts = unreadCounts;
            this.displayNameProvider = displayNameProvider;
            this.isOnlineProvider = isOnlineProvider;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String displayName = displayNameProvider.apply(item);
                Integer unread = unreadCounts.get(item);
                boolean isOnline = isOnlineProvider != null && Boolean.TRUE.equals(isOnlineProvider.apply(item));

                // Status dot (green = online, gray = offline)
                Circle statusDot = new Circle(4.5);
                statusDot.setFill(isOnline ? Color.web("#22c55e") : Color.web("#6b7280"));
                statusDot.setStroke(Color.web("#0f0f12"));   // subtle border for visibility
                statusDot.setStrokeWidth(0.8);

                // Name label (with optional unread count)
                String labelText = (unread != null && unread > 0)
                        ? displayName + " (" + unread + ")"
                        : displayName;

                Label nameLabel = new Label(labelText);

                // Handle selection color properly with custom graphic
                if (isSelected()) {
                    nameLabel.setTextFill(Color.WHITE);
                } else if (unread != null && unread > 0) {
                    nameLabel.setTextFill(Color.web("#f87171"));
                } else {
                    nameLabel.setTextFill(Color.web("#e2e2e7"));
                }

                nameLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 13));

                HBox container = new HBox(8, statusDot, nameLabel);
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                setText(null);
                setGraphic(container);
            }
        }
    }
}