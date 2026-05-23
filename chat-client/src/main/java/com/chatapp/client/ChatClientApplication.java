package com.chatapp.client;

import com.chatapp.client.controller.ChatController;
import com.chatapp.client.network.ChatStompClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Main JavaFX Application for the Chat Client.
 */
public class ChatClientApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        setAppIcons(primaryStage);
        showLoginView();
    }

    /**
     * Show the login view.
     */
    public static void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                ChatClientApplication.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 400, 300);
            scene.getStylesheets().add(
                ChatClientApplication.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setTitle("Chat Client - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            setAppIcons(primaryStage);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the chat view after successful login.
     */
    public static void showChatView(String username, ChatStompClient stompClient) {
        try {
            FXMLLoader loader = new FXMLLoader(
                ChatClientApplication.class.getResource("/fxml/chat.fxml"));
            Parent root = loader.load();

            ChatController controller = loader.getController();
            controller.setConnectedClient(username, stompClient);

            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(
                ChatClientApplication.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setTitle("Chat Client - " + username);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            setAppIcons(primaryStage);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Show error alert and return to login.
     */
    public static void showErrorAndReturn(String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Connection Error");
            alert.setContentText(message);
            alert.showAndWait();
            showLoginView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Sets modern app icons for the stage.
     * Expects icon files in src/main/resources/icons/
     */
    private static void setAppIcons(Stage stage) {
        List<Image> icons = new ArrayList<>();

        // Recommended sizes for best cross-platform results
        String[] sizes = {"16", "32", "64", "128", "256"};

        for (String size : sizes) {
            try {
                String path = "/icons/chat-icon-" + size + ".png";
                Image icon = new Image(ChatClientApplication.class.getResourceAsStream(path));
                if (!icon.isError()) {
                    icons.add(icon);
                }
            } catch (Exception ignored) {
                // Icon not found — will fall back to default
            }
        }

        if (!icons.isEmpty()) {
            stage.getIcons().setAll(icons);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}