package com.chatapp.client.controller;

import com.chatapp.client.ChatClientApplication;
import com.chatapp.client.network.ChatStompClient;
import com.chatapp.common.model.LoginResponse;
import com.chatapp.common.model.SystemMessage;
import com.chatapp.common.model.TextMessage;
import com.chatapp.common.model.UserListMessage;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the login view.
 */
public class LoginController {

    @FXML
    private TextField serverField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private ChatStompClient stompClient;
    private String currentPassword;

    @FXML
    private void initialize() {
        serverField.setText("ws://localhost:8080/ws");
    }

    @FXML
    private void handleLogin() {
        String serverUrl = serverField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        currentPassword = password;

        stompClient = new ChatStompClient(serverUrl);

        stompClient.connect(
            username,
            password,
            this::handleLoginResponse,
            this::handleTextMessage,
            this::handleUserListMessage,
            this::handleSystemMessage
        );
    }

    private void handleLoginResponse(LoginResponse response) {
        if (response.isSuccess()) {
            System.out.println("Login successful");
            ChatClientApplication.showChatView(usernameField.getText().trim(), stompClient);
        } else {
            // Check if it's wrong credentials
            String errorMessage = response.getMessage();
            if (errorMessage != null && errorMessage.toLowerCase().contains("invalid")) {
                showError("Wrong credential");
            } else {
                showError("Login failed: " + errorMessage);
            }
            if (stompClient != null) {
                stompClient.disconnect();
            }
        }
    }

    private void handleTextMessage(TextMessage message) {
        System.out.println("Received message before login: " + message.getContent());
    }

    private void handleUserListMessage(UserListMessage message) {
        System.out.println("Received user list before login");
    }

    private void handleSystemMessage(SystemMessage message) {
        System.out.println("System: " + message.getContent());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Login Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}